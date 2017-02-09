package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.descriptors.allValueParameters
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.RuntimeAware
import org.jetbrains.kotlin.backend.konan.ir.getArguments
import org.jetbrains.kotlin.backend.konan.ir.ir2string
import org.jetbrains.kotlin.backend.konan.llvm.isObjectType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

val DEBUG = 0

// Roles in which particular object reference is being used. Lifetime is computed from
// all roles reference.
internal enum class Role {
    // If reference is created as call result.
    CALL_RESULT,
    // If reference is created as allocation call result.
    ALLOC_RESULT,
    // If reference is being returned.
    RETURN_VALUE,
    // If reference is being thrown.
    THROW_VALUE,
    // If reference's field is being read.
    FIELD_READ,
    // If reference's field is being written to.
    FIELD_WRITTEN,
    // If reference is being read from the field.
    READ_FROM_FIELD,
    // If reference is being written to the field.
    WRITTEN_TO_FIELD,
    // If reference is being read from the gloabl.
    READ_FROM_GLOBAL,
    // If reference is being written to the global.
    WRITTEN_TO_GLOBAL,
    // Outgoing call argument.
    CALL_ARGUMENT
}

internal class RoleInfoEntry(val data: Any? = null)

internal open class RoleInfo {
    val entries = mutableListOf<RoleInfoEntry>()
    open fun add(entry: RoleInfoEntry) = entries.add(entry)
}

private fun RuntimeAware.isInteresting(type: KotlinType) : Boolean =
        !type.isUnit() && !type.isNothing() && isObjectType(type)

internal class Roles {
    val data = HashMap<Role, RoleInfo>()

    fun add(role: Role, info: RoleInfoEntry?) {
        val entry = data.getOrPut(role, { RoleInfo() })
        if (info != null) entry.add(info)
    }

    fun add(roles: Roles) {
        roles.data.forEach { role, info ->
            info.entries.forEach { entry ->
                add(role, entry)
            }
        }
    }

    fun remove(role: Role) = data.remove(role)

    fun has(role: Role) : Boolean = data[role] != null

    fun escapes() : Boolean {
        return has(Role.WRITTEN_TO_FIELD) || has(Role.WRITTEN_TO_GLOBAL) ||
                has(Role.CALL_ARGUMENT) || has(Role.THROW_VALUE)
    }

    fun pure() : Boolean = !escapes() && has(Role.RETURN_VALUE)

    fun info(role: Role) : RoleInfo? = data[role]

    override fun toString() : String {
        val builder = StringBuilder()
        data.forEach { t, u ->
            builder.append(t.name)
            builder.append(": ")
            builder.append(u.entries.joinToString(", "))
            builder.append("; ")
        }
        return builder.toString()
    }
}

internal class VariableValues {
    val elementData = HashMap<ValueDescriptor, MutableSet<IrExpression>>()

    fun addEmpty(variable: ValueDescriptor) =
            elementData.getOrPut(variable, { mutableSetOf<IrExpression>() })
    fun add(variable: ValueDescriptor, element: IrExpression) =
            elementData.get(variable)?.add(element)
    fun add(variable: ValueDescriptor, elements: Set<IrExpression>) =
            elementData.get(variable)?.addAll(elements)
    fun get(variable: ValueDescriptor) : Set<IrExpression>? =
            elementData[variable]

    fun computeClosure() {
        elementData.forEach { key, _ ->
            add(key, computeValueClosure(key))
        }
    }

    // Computes closure of all possible values for given variable.
    fun computeValueClosure(value: ValueDescriptor): Set<IrExpression> {
        val result = mutableSetOf<IrExpression>()
        val workset = mutableSetOf<ValueDescriptor>(value)
        val seen = mutableSetOf<IrGetValue>()
        while (!workset.isEmpty()) {
            val value = workset.first()
            workset -= value
            val elements = elementData[value] ?: continue
            for (element in elements) {
                if (element is IrGetValue) {
                    if (!seen.contains(element)) {
                        seen.add(element)
                        workset.add(element.descriptor)
                    }
                } else {
                    result.add(element)
                }
            }
        }
        return result
    }
}

internal class ParameterRoles {
    val elementData = HashMap<ParameterDescriptor, Roles>()

    fun addParameter(parameter: ParameterDescriptor) {
        elementData.getOrPut(parameter) { Roles() }
    }

    fun add(parameter: ParameterDescriptor, role: Role, roleInfoEntry: RoleInfoEntry?) {
        val roles = elementData.getOrPut(parameter, { Roles() });
        roles.add(role, roleInfoEntry)
    }

    fun canEliminateCallArgument(info: RoleInfo): Boolean {
        for (call in info.entries) {
            val entry = call.data as Pair<ParameterDescriptor, CallableDescriptor>
            val argument = entry.first
            val callee = entry.second
            if (callee is FunctionDescriptor) {
                // Virtual dispatch.
                if (callee.isOverridable) return false
                // Calling external function.
                // TODO: add list of pure external functions in stdlib.
                if (callee.isExternal) return false
            }
            if (DEBUG > 1) println("passed as ${argument}")
            val rolesInCallee = elementData[argument] ?: return false
            if (DEBUG > 1) println("In callee is: $rolesInCallee")
            // TODO: make recursive computation here.
            if (rolesInCallee.escapes()) return false
        }
        return true
    }

    fun propagateCallArguments(roles: Roles): Boolean {
        val info = roles.info(Role.CALL_ARGUMENT) ?: return false
        if (canEliminateCallArgument(info)) {
            roles.remove(Role.CALL_ARGUMENT)
            return true
        }
        return false
    }

    fun propagateCallArguments(expressionToRoles: MutableMap<IrExpression, Roles>) {
        expressionToRoles.forEach {
            if (propagateCallArguments(it.value) && DEBUG > 0)
                println("unescaped expression ${it.key}")
        }
        elementData.forEach {
            if (propagateCallArguments(it.value) && DEBUG > 0)
                println("unescaped paramater ${it.key}")
        }
    }
}

internal fun rolesToLifetime(roles: Roles) : Lifetime {
    val roleSetSize = roles.data.keys.size
    return when {
        // If reference is stored to global or generic field - it must be global.
        roles.escapes() ->
            Lifetime.GLOBAL
         // If we pass it to unknown methods or throw - it must be global.
        roles.has(Role.CALL_ARGUMENT) || roles.has(Role.THROW_VALUE) ->
            Lifetime.GLOBAL
        // If reference is only obtained as call result and never used - it can be local.
        roleSetSize == 1 && roles.has(Role.CALL_RESULT) ->
            Lifetime.LOCAL  // throw Error()
        // If reference is obtained as some call result and returned - we can use return.
        roleSetSize == 2 && roles.has(Role.CALL_RESULT) && roles.has(Role.RETURN_VALUE) ->
            Lifetime.RETURN_VALUE
        // Otherwise, say it is global.
        else ->
            Lifetime.GLOBAL
    }
}

internal class ElementFinderVisitor(
        val context: RuntimeAware,
        val expressionToRoles: MutableMap<IrExpression, Roles>,
        val variableValues: VariableValues,
        val parameterRoles: ParameterRoles)
    : IrElementVisitorVoid {

    fun isInteresting(element: IrExpression) : Boolean {
        return (element is IrMemberAccessExpression && context.isInteresting(element.type)) ||
                (element is IrGetValue && context.isInteresting(element.type))
    }

    fun isInteresting(variable: ValueDescriptor) : Boolean {
        return context.isInteresting(variable.type)
    }

    override fun visitElement(element: IrElement) {
        if (element is IrExpression && isInteresting(element)) {
            expressionToRoles[element] = Roles()
        }
        element.acceptChildrenVoid(this)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        super.visitTypeOperator(expression)
        if (expression.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
            expressionToRoles.remove(expression.argument)
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        declaration.descriptor.allValueParameters.forEach {
            if (isInteresting(it))
                parameterRoles.addParameter(it)
        }
        super.visitFunction(declaration)
    }

    override fun visitVariable(expression: IrVariable) {
        if (isInteresting(expression.descriptor))
            variableValues.addEmpty(expression.descriptor)
        super.visitVariable(expression)
    }
}

//
// elementToRoles is filled with all possible roles given element can play.
// varValues is filled with all possible elements that could be stored in a variable.
//
internal class RoleAssignerVisitor(
        val expressionRoles: MutableMap<IrExpression, Roles>,
        val variableValues: VariableValues,
        val parameterRoles: ParameterRoles,
        val useVarValues: Boolean) : IrElementVisitorVoid {

    fun assignExpressionRole(expression: IrExpression, role: Role, infoEntry: RoleInfoEntry?) {
        if (!useVarValues)
            expressionRoles[expression]?.add(role, infoEntry)
    }

    fun assignValueRole(value: ValueDescriptor, role: Role, infoEntry: RoleInfoEntry?) {
        if (!useVarValues) return
        // Whenever we see variable use in certain role - we propagate this role
        // to all possible expression this variable can be assigned to.
        val possibleValues = variableValues.get(value)
        if (possibleValues != null) {
            for (possibleValue in possibleValues) {
                expressionRoles[possibleValue]?.add(role, infoEntry)
            }
        }
        if (value is ParameterDescriptor) {
            parameterRoles.add(value, role, infoEntry)
        }
    }

    // Here we handle variable assignment.
    fun assignVariable(variable: VariableDescriptor, value: IrExpression) {
        if (useVarValues) return
        // Sometimes we can assign value to Unit variable (unit4.kt) - we don't care.
        if (value.type.isUnit()) return
        when (value) {
            is IrContainerExpression -> assignVariable(variable, value.statements.last() as IrExpression)
            // is IrVararg -> value.elements.forEach { assignVariable(variable, it as IrExpression) }
            is IrWhen -> value.branches.forEach { assignVariable(variable, it.result) }
            is IrMemberAccessExpression -> variableValues.add(variable, value)
            is IrGetValue -> variableValues.add(variable, value)
            is IrGetField -> variableValues.add(variable, value)
            is IrConst<*> -> { }
            is IrTypeOperatorCall -> {
                when (value.operator) {
                    IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.CAST ->
                        assignVariable(variable, value.argument)
                    else -> TODO(ir2string(value))
                }
            }
            is IrTry -> (value.catches + value.tryResult).forEach {
                if (it is IrExpression) assignVariable(variable, it)
            }
            is IrThrow -> { /* Do nothing, error path in an assignment. */ }
            is IrGetObjectValue -> { }
            // TODO: is it correct?
            is IrReturn -> { /* Do nothing, return path in an assignment. */ }
            else -> TODO(ir2string(value))
        }
    }

    // Here we assign a role to expression's value.
    fun assignRole(expression: IrExpression, role: Role, infoEntry: RoleInfoEntry?) {
        if (expression.type.isUnit()) return
        when (expression) {
            is IrContainerExpression -> assignRole(expression.statements.last() as IrExpression, role, infoEntry)
            is IrVararg -> expression.elements.forEach { assignRole(it as IrExpression, role, infoEntry) }
            is IrWhen -> expression.branches.forEach { assignRole(it.result, role, infoEntry) }
            is IrMemberAccessExpression -> assignExpressionRole(expression, role, infoEntry)
            is IrGetValue -> assignValueRole(expression.descriptor, role, infoEntry)
            // If field plays certain role - we cannot use this info now.
            is IrGetField -> {}
            // If constant plays certain role - this information is useless.
            is IrConst<*> -> {}
            is IrTypeOperatorCall -> {
                when (expression.operator) {
                    IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.CAST ->
                        assignRole(expression.argument, role, infoEntry)
                    // No info from those ones.
                    IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF,
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {}
                    else -> TODO(ir2string(expression))
                }
            }
            is IrThrow -> { /* Do nothing, error path in an assignment. */ }
            is IrGetObjectValue -> { /* Shall we do anything here? */ }
            // TODO: is it correct?
            is IrReturn -> { /* Do nothing, return path in an assignment. */ }
            else -> TODO(ir2string(expression))
        }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        assignRole(expression, Role.CALL_RESULT, RoleInfoEntry(expression))
        for (argument in expression.getArguments()) {
            assignRole(argument.second, Role.CALL_ARGUMENT,
                    RoleInfoEntry(Pair(argument.first, expression.descriptor)))
        }
        super.visitCall(expression)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        for (argument in expression.getArguments()) {
            assignRole(argument.second, Role.CALL_ARGUMENT,
                    RoleInfoEntry(Pair(argument.first, expression.descriptor)))
        }
        super.visitDelegatingConstructorCall(expression)
    }

    override fun visitSetField(expression: IrSetField) {
        assignRole(expression.value, Role.WRITTEN_TO_FIELD, RoleInfoEntry(expression))
        super.visitSetField(expression)
    }

    override fun visitField(declaration: IrField) {
        val initializer = declaration.initializer
        if (initializer != null) {
            assignRole(initializer.expression,
                    Role.WRITTEN_TO_FIELD, RoleInfoEntry(declaration))
        }
        super.visitField(declaration)
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        assignVariable(expression.descriptor, expression.value)
        super.visitSetVariable(expression)
    }

    override fun visitVariable(expression: IrVariable) {
        val initializer = expression.initializer
        if (initializer != null) {
            assignVariable(expression.descriptor, initializer)
        }
        super.visitVariable(expression)
    }

    override fun visitReturn(expression: IrReturn) {
        assignRole(expression.value, Role.RETURN_VALUE, RoleInfoEntry(expression))
        super.visitReturn(expression)
    }

    override fun visitThrow(expression: IrThrow) {
        assignRole(expression.value, Role.THROW_VALUE, RoleInfoEntry(expression))
        super.visitThrow(expression)
    }
}

internal class ContextFinderVisitor(val needle: IrElement) : IrElementVisitorVoid {
    val stack = mutableListOf<IrElement>()
    var found = false

    override fun visitElement(element: IrElement) {
        if (found) return
        stack.add(element)
        if (element == needle) {
            found = true
        }
        element.acceptChildrenVoid(this)
        if (found) return
        stack.removeAt(stack.size - 1)
    }

    override fun visitModuleFragment(module: IrModuleFragment) {
        module.acceptChildrenVoid(this)
    }
}

private fun findContext(irModule: IrModuleFragment, element: IrElement) : String {
    val visitor = ContextFinderVisitor(element)
    irModule.acceptVoid(visitor)
    val result = StringBuilder()
    if (visitor.found) {
        visitor.stack.forEach {
            result.append(ir2string(it))
            result.append(" -> ")
        }
    } else {
        result.append("<not found>")
    }
    return result.toString()
}

//
// Analysis we're implementing here is as following.
//  * compute roles IR value nodes can play
//  * merge role information with all methods being called
//  * squash set of roles to conservatively estimated lifetime
//
fun computeLifetimes(irModule: IrModuleFragment, context: RuntimeAware,
                     lifetimes: MutableMap<IrElement, Lifetime>) {
    assert(lifetimes.size == 0)

    val expressionToRoles = mutableMapOf<IrExpression, Roles>()
    val variableValues = VariableValues()
    val parameterRoles = ParameterRoles()
    irModule.acceptVoid(ElementFinderVisitor(
            context, expressionToRoles, variableValues, parameterRoles))
    // On this pass, we collect all possible variable values and assign roles
    // to expressions.
    irModule.acceptVoid(RoleAssignerVisitor(
            expressionToRoles, variableValues, parameterRoles, false))
    // Compute transitive closure of possible values for variables.
    variableValues.computeClosure()
    // On this pass, we use possible variable values to assign roles to expression.
    irModule.acceptVoid(RoleAssignerVisitor(
            expressionToRoles, variableValues, parameterRoles, true))
    parameterRoles.propagateCallArguments(expressionToRoles)

    expressionToRoles.forEach { expression, roles ->
        if (DEBUG > 1)
            println("for ${expression} roles are ${roles.data.keys.joinToString(", ")}")
        var role = rolesToLifetime(roles)
        if (role == Lifetime.LOCAL) {
            if (DEBUG > 0)
                println("arena alloc for ${ir2string(expression)} in ${findContext(irModule, expression)}")
            // Not yet enabled.
            role = Lifetime.GLOBAL
        }
        lifetimes[expression] = role
    }
    if (DEBUG > 1) {
        variableValues.elementData.forEach { variable, expressions ->
            println("variable $variable could be ${expressions.joinToString(", ")}") }
        parameterRoles.elementData.forEach { parameter, roles ->
            println("parameter $parameter could be $roles") }
    }
}