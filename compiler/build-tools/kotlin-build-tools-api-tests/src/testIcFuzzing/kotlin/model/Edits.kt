/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing.model

/**
 * An edit of a [Program]. The targets of the edit are chosen at application time from [choices],
 * so an edit stays applicable (or becomes a no-op) when jqwik shrinks the edits applied before it.
 */
data class Edit(val kind: EditKind, val choices: List<Int>) {
    override fun toString() = "$kind$choices"
}

class EditOutcome(val program: Program, val description: String, val touchedFiles: Set<String> = emptySet())

/**
 * The kinds of edits. [initialWeight] is used for generating the initial program (0 means the kind is not used there),
 * [weight] is used for the edits applied after the initial build.
 */
enum class EditKind(val initialWeight: Int, val weight: Int) {
    ADD_FILE(1, 1),
    ADD_FUN(4, 3),
    ADD_PROP(2, 2),
    ADD_TYPEALIAS(1, 1),
    ADD_CLASS(4, 2),
    ADD_SEALED_SUBCLASS(1, 2),
    ADD_MEMBER(2, 2),
    ADD_OVERRIDE(1, 2),
    ADD_USE(4, 3),
    ADD_ENUM_ENTRY(1, 1),
    ADD_SPECIFIC_OVERLOAD(1, 2),
    ADD_INTERFACE(1, 1),
    ADD_NESTED_CLASS(1, 1),
    ADD_SHADOWING_FUN(0, 1),
    ADD_SHADOWING_MEMBER(0, 1),

    CHANGE_FUN_BODY(0, 3),
    TOGGLE_INLINE(0, 2),
    TOGGLE_SUSPEND(0, 2),
    CHANGE_LAMBDA_PARAM(0, 2),
    CHANGE_DEAD_BRANCH(0, 2),
    CHANGE_OWNED_OBJECT(0, 2),
    CHANGE_RETURN_TYPE(0, 3),
    TOGGLE_IMPLICIT_TYPE(0, 2),
    ADD_PARAM(0, 2),
    REMOVE_PARAM(0, 1),
    TOGGLE_PARAM_DEFAULT(0, 1),
    CHANGE_PARAM_DEFAULT_VALUE(0, 2),
    CHANGE_PARAM_TYPE(0, 1),
    RENAME_PARAM(0, 1),
    CHANGE_CALL_LAMBDA(0, 1),
    TOGGLE_DEPRECATION_CONST(0, 1),
    CHANGE_RECEIVER(0, 1),
    TOGGLE_INFIX(0, 1),
    TOGGLE_OPERATOR(0, 1),
    CHANGE_FUN_JVM_NAME(0, 1),
    TOGGLE_JVM_OVERLOADS(0, 1),
    TOGGLE_PUBLISHED_API(0, 1),
    TOGGLE_HIDDEN(0, 1),
    TOGGLE_CONTRACT(0, 1),
    CHANGE_LOCAL_CLASS(0, 1),
    CHANGE_LOCAL_FUN(0, 1),
    CHANGE_LAMBDA_SHAPE(0, 1),
    TOGGLE_JVM_SYNTHETIC(0, 1),
    TOGGLE_THROWS(0, 1),
    CHANGE_TYPE_PARAM(0, 1),
    TOGGLE_TYPE_PARAM_BOUND(0, 1),
    CHANGE_VISIBILITY(0, 2),
    CHANGE_PROP_VALUE(0, 2),
    CHANGE_PROP_KIND(0, 1),
    CHANGE_PROP_TYPE(0, 1),
    CHANGE_PROP_ACCESSOR(0, 1),
    CHANGE_PROP_RECEIVER(0, 1),
    TOGGLE_JVM_FIELD(0, 1),
    TOGGLE_VOLATILE(0, 1),
    CHANGE_ALIAS_TARGET(0, 2),
    CHANGE_CLASS_KIND(0, 2),
    CHANGE_SUPER(0, 2),
    REMOVE_INTERFACE(0, 1),
    REORDER_SUPERTYPES(0, 1),
    TOGGLE_INNER(0, 1),
    MOVE_NESTED_OUT(0, 1),
    TOGGLE_JVM_STATIC(0, 1),
    CHANGE_ENUM_CONSTRUCTOR(0, 1),
    CHANGE_ENUM_ENTRY_BODIES(0, 1),
    TOGGLE_FUN_INTERFACE(0, 1),
    RENAME_ENUM_ENTRY(0, 1),
    CHANGE_MEMBER_MODALITY(0, 2),
    ADD_MEMBER_PARAM(0, 1),
    TOGGLE_MEMBER_PROPERTY(0, 1),
    CHANGE_MEMBER_VISIBILITY(0, 1),
    TOGGLE_COVARIANT_RETURN(0, 1),
    CHANGE_MEMBER_PROP_KIND(0, 1),
    CHANGE_MEMBER_ACCESSOR(0, 1),
    TOGGLE_MEMBER_JVM_FIELD(0, 1),
    TOGGLE_FINAL_OVERRIDE(0, 1),
    TOGGLE_OVERRIDE_OTHER_KIND(0, 1),
    REMOVE_MEMBER(0, 1),
    REMOVE_USE(0, 1),
    REMOVE_ENUM_ENTRY(0, 1),
    REMOVE_DECL(0, 1),
    RENAME_DECL(0, 1),
    MOVE_DECL(0, 2),
    CHANGE_PACKAGE(0, 1),
    RENAME_FILE(0, 1),
    DELETE_FILE(0, 1),
    TOUCH_FILE(0, 1),
    CHANGE_COMMENT(0, 1),
    SHIFT_LINES(0, 1),
    REORDER_DECLS(0, 1),
    REORDER_MEMBERS(0, 1),
    TOGGLE_STAR_IMPORTS(0, 1),
    TOGGLE_IMPORT_ALIAS(0, 1),
    CHANGE_FILE_JVM_NAME(0, 1),
    TOGGLE_MULTIFILE(0, 1),
}

object Edits {
    const val CHOICES_PER_EDIT = 12

    private val PACKAGES = listOf("p0", "p1", "p2")

    private val ANY_TYPE: TypeRef = TypeRef.BuiltinType(Builtin.ANY)

    private val PRIMITIVES = setOf(Builtin.INT, Builtin.LONG, Builtin.BOOLEAN)

    /** The program the initial edits are applied to. */
    val SEED_PROGRAM = Program(
        files = listOf(SourceFile("f1.kt", "p0", emptyList()), SourceFile("f2.kt", "p0", emptyList()), SourceFile("f3.kt", "p1", emptyList())),
        nextId = 4,
    )

    /** Applies [edit] to [program], returns `null` if the edit has no target in [program]. */
    fun apply(program: Program, edit: Edit): EditOutcome? = with(EditContext(program, Choices(edit.choices))) {
        when (edit.kind) {
            EditKind.ADD_FILE -> addFile()
            EditKind.ADD_FUN -> addFun()
            EditKind.ADD_PROP -> addProp()
            EditKind.ADD_TYPEALIAS -> addTypeAlias()
            EditKind.ADD_CLASS -> addClass()
            EditKind.ADD_SEALED_SUBCLASS -> addSealedSubclass()
            EditKind.ADD_MEMBER -> addMember()
            EditKind.ADD_OVERRIDE -> addOverride()
            EditKind.ADD_USE -> addUse()
            EditKind.ADD_ENUM_ENTRY -> addEnumEntry()
            EditKind.ADD_SPECIFIC_OVERLOAD -> addSpecificOverload()
            EditKind.ADD_INTERFACE -> addInterface()
            EditKind.ADD_NESTED_CLASS -> addNestedClass()
            EditKind.ADD_SHADOWING_FUN -> addShadowingFun()
            EditKind.ADD_SHADOWING_MEMBER -> addShadowingMember()
            EditKind.CHANGE_FUN_BODY -> changeFunBody()
            EditKind.TOGGLE_INLINE -> toggleInline()
            EditKind.TOGGLE_SUSPEND -> toggleSuspend()
            EditKind.CHANGE_LAMBDA_PARAM -> changeLambdaParam()
            EditKind.CHANGE_DEAD_BRANCH -> changeDeadBranch()
            EditKind.CHANGE_OWNED_OBJECT -> changeOwnedObject()
            EditKind.CHANGE_RETURN_TYPE -> changeReturnType()
            EditKind.TOGGLE_IMPLICIT_TYPE -> toggleImplicitType()
            EditKind.ADD_PARAM -> addParam()
            EditKind.REMOVE_PARAM -> removeParam()
            EditKind.TOGGLE_PARAM_DEFAULT -> toggleParamDefault()
            EditKind.CHANGE_PARAM_DEFAULT_VALUE -> changeParamDefaultValue()
            EditKind.CHANGE_PARAM_TYPE -> changeParamType()
            EditKind.RENAME_PARAM -> renameParam()
            EditKind.CHANGE_CALL_LAMBDA -> changeCallLambda()
            EditKind.TOGGLE_DEPRECATION_CONST -> toggleDeprecationConst()
            EditKind.CHANGE_RECEIVER -> changeReceiver()
            EditKind.TOGGLE_INFIX -> toggleInfix()
            EditKind.TOGGLE_OPERATOR -> toggleOperator()
            EditKind.CHANGE_FUN_JVM_NAME -> changeFunJvmName()
            EditKind.TOGGLE_JVM_OVERLOADS -> toggleFun("@JvmOverloads") { it.copy(jvmOverloads = !it.jvmOverloads) }
            EditKind.TOGGLE_PUBLISHED_API -> toggleFun("@PublishedApi") { it.copy(publishedApi = !it.publishedApi) }
            EditKind.TOGGLE_HIDDEN -> toggleFun("the hidden deprecation") { it.copy(hidden = !it.hidden) }
            EditKind.TOGGLE_CONTRACT -> toggleFun("the contract") { it.copy(contract = !it.contract) }
            EditKind.CHANGE_LOCAL_CLASS -> toggleFun("the local class") { it.copy(localClassVariant = nextVariant(it.localClassVariant)) }
            EditKind.CHANGE_LOCAL_FUN -> toggleFun("the local function") { it.copy(localFunVariant = nextVariant(it.localFunVariant)) }
            EditKind.CHANGE_LAMBDA_SHAPE -> toggleFun("the type of the lambda parameter") { it.copy(lambdaShape = choices.pick(LambdaShape.entries - it.lambdaShape)!!) }
            EditKind.TOGGLE_JVM_SYNTHETIC -> toggleFun("@JvmSynthetic") { it.copy(jvmSynthetic = !it.jvmSynthetic) }
            EditKind.TOGGLE_THROWS -> toggleFun("@Throws") { it.copy(throws = !it.throws) }
            EditKind.CHANGE_TYPE_PARAM -> changeTypeParam()
            EditKind.TOGGLE_TYPE_PARAM_BOUND -> toggleFun("the bound of the type parameter") { it.copy(typeParamNonNull = !it.typeParamNonNull) }
            EditKind.CHANGE_VISIBILITY -> changeVisibility()
            EditKind.CHANGE_PROP_VALUE -> changePropValue()
            EditKind.CHANGE_PROP_KIND -> changePropKind()
            EditKind.CHANGE_PROP_TYPE -> changePropType()
            EditKind.CHANGE_PROP_ACCESSOR -> changePropAccessor()
            EditKind.CHANGE_PROP_RECEIVER -> changePropReceiver()
            EditKind.TOGGLE_JVM_FIELD -> toggleJvmField()
            EditKind.TOGGLE_VOLATILE -> toggleVolatile()
            EditKind.CHANGE_ALIAS_TARGET -> changeAliasTarget()
            EditKind.CHANGE_CLASS_KIND -> changeClassKind()
            EditKind.CHANGE_SUPER -> changeSuper()
            EditKind.REMOVE_INTERFACE -> removeInterface()
            EditKind.REORDER_SUPERTYPES -> reorderSupertypes()
            EditKind.TOGGLE_INNER -> toggleInner()
            EditKind.MOVE_NESTED_OUT -> moveNestedOut()
            EditKind.TOGGLE_JVM_STATIC -> toggleJvmStatic()
            EditKind.CHANGE_ENUM_CONSTRUCTOR -> changeEnumConstructor()
            EditKind.CHANGE_ENUM_ENTRY_BODIES -> changeEnumEntryBodies()
            EditKind.TOGGLE_FUN_INTERFACE -> toggleFunInterface()
            EditKind.RENAME_ENUM_ENTRY -> renameEnumEntry()
            EditKind.CHANGE_MEMBER_MODALITY -> changeMemberModality()
            EditKind.ADD_MEMBER_PARAM -> addMemberParam()
            EditKind.TOGGLE_MEMBER_PROPERTY -> toggleMemberProperty()
            EditKind.CHANGE_MEMBER_VISIBILITY -> changeMemberVisibility()
            EditKind.TOGGLE_COVARIANT_RETURN -> toggleCovariantReturn()
            EditKind.CHANGE_MEMBER_PROP_KIND -> updateRootMember("the kind of the property") { it.copy(propKind = choices.pick(PropKind.entries - it.propKind)!!) }
            EditKind.CHANGE_MEMBER_ACCESSOR -> updateAnyMember("the accessor") { it.copy(accessor = if (it.accessor == PropAccessor.FIELD) PropAccessor.GETTER else PropAccessor.FIELD) }
            EditKind.TOGGLE_MEMBER_JVM_FIELD -> updateRootMember("@JvmField") { it.copy(jvmField = !it.jvmField) }
            EditKind.TOGGLE_FINAL_OVERRIDE -> updateOverride("final") { it.copy(finalOverride = !it.finalOverride) }
            EditKind.TOGGLE_OVERRIDE_OTHER_KIND -> updateOverride("the kind") { it.copy(overridesWithOtherKind = !it.overridesWithOtherKind) }
            EditKind.REMOVE_MEMBER -> removeMember()
            EditKind.REMOVE_USE -> removeUse()
            EditKind.REMOVE_ENUM_ENTRY -> removeEnumEntry()
            EditKind.REMOVE_DECL -> removeDecl()
            EditKind.RENAME_DECL -> renameDecl()
            EditKind.MOVE_DECL -> moveDecl()
            EditKind.CHANGE_PACKAGE -> changePackage()
            EditKind.RENAME_FILE -> renameFile()
            EditKind.DELETE_FILE -> deleteFile()
            EditKind.TOUCH_FILE -> touchFile()
            EditKind.CHANGE_COMMENT -> changeComment()
            EditKind.SHIFT_LINES -> shiftLines()
            EditKind.REORDER_DECLS -> reorderDecls()
            EditKind.REORDER_MEMBERS -> reorderMembers()
            EditKind.TOGGLE_STAR_IMPORTS -> toggleStarImports()
            EditKind.TOGGLE_IMPORT_ALIAS -> toggleImportAlias()
            EditKind.CHANGE_FILE_JVM_NAME -> changeFileJvmName()
            EditKind.TOGGLE_MULTIFILE -> toggleMultifile()
        }
    }

    /**
     * Repairs what the later initial edits broke in the earlier declarations: makes the `when` uses exhaustive again
     * and implements the abstract members added to supertypes.
     */
    fun completeInitialProgram(program: Program): Program = EditContext(program, Choices(emptyList())).complete()

    private class Choices(private val values: List<Int>) {
        private var index = 0

        fun next(bound: Int): Int {
            if (bound <= 1 || values.isEmpty()) return 0
            return values[index++ % values.size].mod(bound)
        }

        fun chance(percent: Int): Boolean = next(100) < percent

        fun <T> pick(list: List<T>): T? = if (list.isEmpty()) null else list[next(list.size)]
    }

    private class EditContext(var program: Program, val choices: Choices) {
        private val funs get() = program.decls.filterIsInstance<FunDecl>()
        private val props get() = program.decls.filterIsInstance<PropDecl>()
        private val aliases get() = program.decls.filterIsInstance<TypeAliasDecl>()
        private val classes get() = program.classes

        private fun newId(): Int = program.withNextId().let { [newProgram, id] -> program = newProgram; id }

        private fun pickFile(): SourceFile? = choices.pick(program.files)

        private fun addDecl(file: SourceFile, decl: Decl) {
            program = program.replaceFile(file.name) { it.copy(decls = it.decls + decl) }
        }

        private fun outcome(description: String, touchedFiles: Set<String> = emptySet()) = EditOutcome(program, description, touchedFiles)

        private fun describe(decl: Decl) = "${decl.name} in ${program.fileOf(decl.id)?.name}"

        // ---- random fragments ----

        /** `Any` is not a type of a constant. */
        private fun randomBuiltin(allowAny: Boolean = true): TypeRef {
            val builtins = if (allowAny) Builtin.entries else Builtin.entries - Builtin.ANY
            return TypeRef.BuiltinType(builtins[choices.next(builtins.size)])
        }

        private fun randomType(maxId: Int = Int.MAX_VALUE): TypeRef {
            val roll = choices.next(10)
            val type = when {
                roll >= 8 -> aliases.filter { it.id < maxId }.let(choices::pick)?.let { TypeRef.AliasType(it.id) }
                roll >= 6 -> classes.filter { it.id < maxId }.let(choices::pick)?.let { TypeRef.ClassType(it.id) }
                else -> null
            } ?: randomBuiltin()
            return if (choices.chance(15)) type.withNullable(true) else type
        }

        private fun randomParams(): List<Param> =
            List(choices.next(3)) { index -> Param("a$index", randomType(), hasDefault = choices.chance(50)) }

        /** A type argument within the bound of the type parameter of [decl]. */
        private fun randomTypeArg(decl: FunDecl): TypeRef = randomBuiltin().let { if (!decl.typeParamNonNull && choices.chance(15)) it.withNullable(true) else it }

        /** A parameter of the type parameter, which has no default value. */
        private fun typeParamParam(name: String): Param = Param(name, TypeRef.TypeParamType(nullable = choices.chance(20)), hasDefault = false)

        /** The member functions callable on an instance of [cls], including the inherited ones. */
        private fun callableMembers(cls: ClassDecl): List<MemberFun> = program.supertypes(cls.id).flatMap { it.members }

        /**
         * A use in the body of the function [ownerId]. Only the functions declared before the owner are called,
         * so that the call graph is acyclic and making a function inline never makes it recursive.
         * As a call may resolve to any overload, all functions with the name of the callee have to be declared before the owner.
         * Suspend functions are called only from a suspend owner.
         */
        private fun randomUse(ownerId: Int, ownerName: String? = null, ownerSuspend: Boolean = false): Use? {
            val candidates = buildList<() -> Use?> {
                val laterNames = funs.filter { it.id >= ownerId }.map { it.name }.toSet() + listOfNotNull(ownerName)
                funs.filter { it.id < ownerId && it.name !in laterNames && (ownerSuspend || !it.isSuspend) }.takeIf { it.isNotEmpty() }?.let {
                    add {
                        choices.pick(it)?.let { decl ->
                            val form = when {
                                decl.isBinary && decl.operator && choices.chance(50) -> CallForm.OPERATOR
                                decl.isBinary && decl.infix && choices.chance(50) -> CallForm.INFIX
                                else -> CallForm.REGULAR
                            }
                            Use.CallFun(decl.id, decl.params.map(Param::name), named = choices.chance(80), form = form)
                                .let { use -> if (decl.typeParam == null) use else use.copy(typeArg = randomTypeArg(decl)) }
                        }
                    }
                }
                props.takeIf { it.isNotEmpty() }?.let { add { choices.pick(it)?.let { decl -> Use.ReadProp(decl.id) } } }
                props.takeIf { it.isNotEmpty() }?.let { add { choices.pick(it)?.let { decl -> Use.WhenConst(decl.id) } } }
                funs.filter { it.id < ownerId && it.name !in laterNames && it.hasContract }.takeIf { it.isNotEmpty() }?.let {
                    add { choices.pick(it)?.let { decl -> Use.ContractSmartCast(decl.id) } }
                }
                val instantiable = classes.filter { it.isInstantiable }
                instantiable.takeIf { it.isNotEmpty() }?.let { add { choices.pick(it)?.let { cls -> Use.Construct(cls.id) } } }
                instantiable.filter { callableMembers(it).isNotEmpty() }.takeIf { it.isNotEmpty() }?.let {
                    add {
                        choices.pick(it)?.let { cls ->
                            choices.pick(callableMembers(cls))?.let { member ->
                                val root = program.member(rootOf(member))?.second ?: member
                                Use.CallMember(cls.id, member.id, root.params.map(Param::name), named = choices.chance(80))
                            }
                        }
                    }
                }
                classes.filter { cls -> cls.kind.isSealed && program.directSubclasses(cls.id).any { it.isInstantiable } }
                    .takeIf { it.isNotEmpty() }?.let {
                        add {
                            choices.pick(it)?.let { sealed ->
                                val subclasses = program.directSubclasses(sealed.id)
                                val scrutinee = choices.pick(subclasses.filter { sub -> sub.isInstantiable }) ?: return@let null
                                Use.WhenSealed(sealed.id, scrutinee.id, subclasses.map { sub -> sub.id })
                            }
                        }
                    }
                classes.filter { it.kind == ClassKind.ENUM && it.enumEntries.isNotEmpty() }.takeIf { it.isNotEmpty() }?.let {
                    add { choices.pick(it)?.let { enum -> Use.WhenEnum(enum.id, enum.enumEntries, viaValues = choices.chance(30)) } }
                }
                classes.filter { it.kind == ClassKind.INTERFACE && it.funInterface && it.samMember != null }.takeIf { it.isNotEmpty() }?.let {
                    add { choices.pick(it)?.let { cls -> Use.SamConversion(cls.id) } }
                }
                props.filter { it.type.nullable }.takeIf { it.isNotEmpty() }?.let { add { choices.pick(it)?.let { prop -> Use.SmartCast(prop.id) } } }
                val objectProperties = classes.filter { it.kind == ClassKind.OBJECT }.flatMap { obj ->
                    callableMembers(obj).filter { (program.member(rootOf(it))?.second ?: it).isProperty }.map { obj to it }
                }
                objectProperties.filter { [_, member] -> (program.member(rootOf(member))?.second ?: member).returnType.nullable }
                    .takeIf { it.isNotEmpty() }?.let { add { choices.pick(it)?.let { [obj, member] -> Use.SmartCastMember(obj.id, member.id) } } }
                objectProperties.takeIf { it.isNotEmpty() }?.let { add { choices.pick(it)?.let { [obj, member] -> Use.WhenMember(obj.id, member.id) } } }
            }
            return choices.pick(candidates)?.invoke()
        }

        private fun randomUses(ownerId: Int, max: Int, ownerName: String? = null, ownerSuspend: Boolean = false): List<Use> =
            List(choices.next(max + 1)) { randomUse(ownerId, ownerName, ownerSuspend) }.filterNotNull()

        private fun rootOf(member: MemberFun): Int {
            val visited = mutableSetOf<Int>()
            var current = member
            while (current.overrides != null && visited.add(current.id)) {
                current = program.member(current.overrides)?.second ?: return current.overrides
            }
            return current.id
        }

        /** The abstract members of [classIds] and their supertypes which are not implemented by any of them. */
        private fun unimplementedAbstractMembers(classIds: List<Int>): List<MemberFun> {
            val members = classIds.flatMap(program::supertypes).distinct().flatMap { it.members }
            val implemented = members.filter { it.overrides != null }.map(::rootOf).toSet()
            return members.filter { it.modality == Modality.ABSTRACT && it.overrides == null && it.id !in implemented }
        }

        private fun overrideOf(member: MemberFun) = MemberFun(
            id = newId(), name = member.name, modality = Modality.OPEN, overrides = member.id,
            returnType = member.returnType, bodyVariant = choices.next(100), uses = emptyList(),
        )

        private fun randomMember(owner: ClassKind): MemberFun {
            val modalities = buildList {
                add(Modality.FINAL)
                if (owner.isInheritable) add(Modality.OPEN)
                if (owner.allowsAbstractMembers) add(Modality.ABSTRACT)
            }
            val modality = choices.pick(modalities)!!
            val id = newId()
            val isProperty = choices.chance(25)
            return MemberFun(
                id = id, name = "m$id", modality = modality, overrides = null, returnType = randomType(),
                bodyVariant = choices.next(100), uses = if (modality == Modality.ABSTRACT) emptyList() else randomUses(id, 1),
                params = if (isProperty) emptyList() else randomParams(), isProperty = isProperty,
                propKind = if (isProperty && choices.chance(40)) choices.pick(PropKind.entries)!! else PropKind.VAL,
                accessor = if (isProperty && choices.chance(40)) PropAccessor.FIELD else PropAccessor.GETTER,
                jvmField = choices.chance(10),
            )
        }

        /** An inner class cannot be extended outside its outer class. */
        private fun superCandidates(kind: ClassKind, maxId: Int): List<ClassDecl> = classes.filter { candidate ->
            candidate.id < maxId && (candidate.nesting != Nesting.INNER || candidate.outerId == null) && when (kind) {
                ClassKind.INTERFACE, ClassKind.SEALED_INTERFACE, ClassKind.ENUM, ClassKind.VALUE -> candidate.kind == ClassKind.INTERFACE
                else -> candidate.kind.isInheritable
            }
        }

        private fun newClass(
            file: SourceFile,
            kind: ClassKind,
            superId: Int?,
            interfaceIds: List<Int> = emptyList(),
            outerId: Int? = null,
            nesting: Nesting = Nesting.NESTED,
        ): ClassDecl {
            val id = newId()
            val overrides = if (kind.allowsAbstractMembers) emptyList() else unimplementedAbstractMembers(listOfNotNull(superId) + interfaceIds).map(::overrideOf)
            val members = overrides + List(choices.next(3)) { randomMember(kind) }
            val entries = if (kind == ClassKind.ENUM) List(1 + choices.next(3)) { "E$it" } else emptyList()
            val cls = ClassDecl(
                id, "C$id", Visibility.PUBLIC, kind, superId, members, entries, interfaceIds, outerId, nesting,
                enumConstructorVariant = if (kind == ClassKind.ENUM && choices.chance(30)) 1 else 0,
                enumEntryBodyVariant = if (kind == ClassKind.ENUM && choices.chance(30)) 1 else 0,
                funInterface = kind == ClassKind.INTERFACE && choices.chance(30),
            )
            addDecl(file, cls)
            return cls
        }

        // ---- additions ----

        fun addFile(): EditOutcome {
            val id = newId()
            val file = SourceFile("f$id.kt", choices.pick(PACKAGES)!!, emptyList())
            program = program.copy(files = program.files + file)
            return outcome("add file ${file.name} in package ${file.pkg}")
        }

        fun addFun(): EditOutcome? {
            val file = pickFile() ?: return null
            val id = newId()
            val typeParam = if (choices.chance(15)) choices.pick(TypeParamKind.entries) else null
            var params = randomParams().map { if (typeParam != null && choices.chance(30)) typeParamParam(it.name) else it }
            val lambda = if (choices.chance(25)) choices.pick(LambdaKind.entries) else null
            // A call passes the arguments without defaults, so an overload and the functions with the same name have no defaults
            // and differ in the number of arguments: then a call is neither conflicting nor ambiguous.
            val overloaded = if (choices.chance(20)) {
                choices.pick(funs)?.takeIf { candidate ->
                    funs.filter { it.name == candidate.name }.none { it.params.any(Param::hasDefault) || it.arity == params.size + lambda.arity }
                }
            } else null
            if (overloaded != null) params = params.map { it.copy(hasDefault = false) }
            // the earlier functions may already call an overload of this name, so an inline overload could create an inline cycle
            val inline = overloaded == null && choices.chance(20)
            // the earlier non-suspend functions may already call an overload of this name
            val suspend = overloaded?.isSuspend ?: choices.chance(15)
            val name = overloaded?.name ?: "f$id"
            val receiver = if (choices.chance(15)) choices.pick(classes) else null
            val decl = FunDecl(
                id = id, name = name, visibility = Visibility.PUBLIC, inline = inline,
                params = params, returnType = randomType(), bodyVariant = choices.next(100), uses = emptyList(),
                suspend = suspend,
                lambda = lambda,
                deadBranchVariant = if (choices.chance(20)) 1 else 0,
                ownedObjectVariant = if (choices.chance(20)) 1 else 0,
                implicitReturnType = choices.chance(20),
                receiverClassId = receiver?.id,
                infix = choices.chance(30),
                jvmNameVariant = if (choices.chance(10)) 1 else 0,
                jvmOverloads = choices.chance(10),
                contract = choices.chance(20),
                typeParam = typeParam,
                typeParamNonNull = choices.chance(50),
                lambdaShape = if (choices.chance(30)) choices.pick(LambdaShape.entries)!! else LambdaShape.NO_PARAMS,
                localClassVariant = if (choices.chance(15)) 1 else 0,
                localFunVariant = if (choices.chance(15)) 1 else 0,
                jvmSynthetic = choices.chance(5),
                throws = choices.chance(10),
            ).let { it.copy(uses = randomUses(id, 2, name, it.isSuspend)) }
            addDecl(file, decl)
            val receiverName = receiver?.let { "${it.name}." } ?: ""
            return outcome("add ${modifiers(decl)}fun $receiverName${decl.name}${if (overloaded != null) " (overload)" else ""} to ${file.name}")
        }

        private fun modifiers(decl: FunDecl) = (if (decl.inline) "inline " else "") + (if (decl.isSuspend) "suspend " else "")

        /** The number of the arguments of a call. */
        private val FunDecl.arity: Int get() = params.size + lambda.arity

        private val LambdaKind?.arity: Int get() = if (this == null) 0 else 1

        fun addProp(): EditOutcome? {
            val file = pickFile() ?: return null
            val id = newId()
            val kind = PropKind.entries[choices.next(PropKind.entries.size)]
            val type = if (kind == PropKind.CONST) randomBuiltin(allowAny = false) else randomType()
            val prop = PropDecl(
                id, "p$id", Visibility.PUBLIC, kind, type, choices.next(100), implicitType = choices.chance(20),
                accessor = if (choices.chance(25)) choices.pick(PropAccessor.entries)!! else PropAccessor.FIELD,
                receiverClassId = if (choices.chance(10)) choices.pick(classes)?.id else null,
                jvmField = choices.chance(10),
                volatile = choices.chance(10),
            )
            addDecl(file, prop)
            return outcome("add ${kind.name.lowercase()} p$id to ${file.name}")
        }

        fun addTypeAlias(): EditOutcome? {
            val file = pickFile() ?: return null
            val id = newId()
            addDecl(file, TypeAliasDecl(id, "A$id", Visibility.PUBLIC, randomType(maxId = id)))
            return outcome("add typealias A$id to ${file.name}")
        }

        fun addClass(): EditOutcome? {
            val kind = ClassKind.entries[choices.next(ClassKind.entries.size)]
            val superClass = if (choices.chance(50)) choices.pick(superCandidates(kind, Int.MAX_VALUE)) else null
            val file = if (superClass?.kind?.isSealed == true) {
                choices.pick(program.files.filter { it.pkg == program.fileOf(superClass.id)?.pkg })
            } else {
                pickFile()
            } ?: return null
            val interfaces = if (choices.chance(30)) listOfNotNull(choices.pick(interfaceCandidates(Int.MAX_VALUE, superClass?.id))) else emptyList()
            val cls = newClass(file, kind, superClass?.id, interfaces.map { it.id })
            val supertypes = (listOfNotNull(superClass) + interfaces).joinToString { it.name }.ifEmpty { null }
            return outcome("add ${kind.name.lowercase()} class ${cls.name}${supertypes?.let { " : $it" } ?: ""} to ${file.name}")
        }

        /** The interfaces which may be added to the supertypes of the class [classId] declared after them, other than [except]. */
        private fun interfaceCandidates(classId: Int, except: Int?): List<ClassDecl> =
            classes.filter { it.kind == ClassKind.INTERFACE && it.id < classId && it.id != except }

        fun addSealedSubclass(): EditOutcome? {
            val sealed = choices.pick(classes.filter { it.kind.isSealed }) ?: return null
            val file = choices.pick(program.files.filter { it.pkg == program.fileOf(sealed.id)?.pkg }) ?: return null
            val kind = choices.pick(listOf(ClassKind.FINAL, ClassKind.OBJECT, ClassKind.DATA, ClassKind.SEALED))!!
            val cls = newClass(file, kind, sealed.id)
            return outcome("add ${kind.name.lowercase()} subclass ${cls.name} of sealed ${sealed.name} to ${file.name}")
        }

        /** Adds a class to the body of another class. It is declared in the same file in the model, to stay there if it is moved out. */
        fun addNestedClass(): EditOutcome? {
            val outer = choices.pick(classes.filter { it.nesting != Nesting.INNER || it.outerId == null }) ?: return null
            val file = program.fileOf(outer.id) ?: return null
            val nesting = choices.pick(Nesting.entries)!!
            val kind = if (nesting == Nesting.COMPANION) ClassKind.OBJECT else choices.pick(ClassKind.entries - ClassKind.SEALED_INTERFACE)!!
            val superClass = if (choices.chance(30)) choices.pick(superCandidates(kind, Int.MAX_VALUE).filter { !it.kind.isSealed }) else null
            val cls = newClass(file, kind, superClass?.id, outerId = outer.id, nesting = nesting)
            return outcome("add ${nesting.name.lowercase()} ${kind.name.lowercase()} class ${cls.name} to ${describe(outer)}")
        }

        fun addMember(): EditOutcome? {
            val cls = choices.pick(classes) ?: return null
            val member = randomMember(cls.kind)
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(members = it.members + member) }
            return outcome("add ${member.modality.name.lowercase()} member ${member.name} to ${describe(cls)}")
        }

        fun addOverride(): EditOutcome? {
            val candidates = classes.flatMap { cls ->
                val chain = program.supertypes(cls.id)
                val alreadyOverridden = cls.members.mapNotNull { it.overrides }.toSet()
                chain.drop(1).flatMap { superClass ->
                    superClass.members.filter { member ->
                        member.overrides == null && member.id !in alreadyOverridden &&
                                (member.modality != Modality.FINAL || superClass.kind.isInterface)
                    }
                }.map { cls to it }
            }
            val [cls, overridden] = choices.pick(candidates) ?: return null
            val member = overrideOf(overridden)
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(members = it.members + member) }
            return outcome("override ${overridden.name} in ${describe(cls)}")
        }

        fun addUse(): EditOutcome? {
            val targets = funs.map { it.id to null } + classes.flatMap { cls ->
                cls.members.filter { it.modality != Modality.ABSTRACT || it.overrides != null }.map { cls.id to it.id }
            }
            val [declId, memberId] = choices.pick(targets) ?: return null
            val ownerSuspend = memberId == null && (program.decl(declId) as? FunDecl)?.isSuspend == true
            val use = randomUse(ownerId = memberId ?: declId, ownerSuspend = ownerSuspend) ?: return null
            program = program.replaceDecl(declId) { decl ->
                when (decl) {
                    is FunDecl -> decl.copy(uses = decl.uses + use)
                    is ClassDecl -> decl.replaceMember(memberId!!) { it.copy(uses = it.uses + use) }
                    else -> decl
                }
            }
            return outcome("add use $use to ${program.decl(declId)?.name}${memberId?.let { ".m$it" } ?: ""}")
        }

        fun addEnumEntry(): EditOutcome? {
            val enum = choices.pick(classes.filter { it.kind == ClassKind.ENUM }) ?: return null
            val entry = "E${newId()}"
            program = program.replaceDecl(enum.id) { (it as ClassDecl).copy(enumEntries = it.enumEntries + entry) }
            return outcome("add entry $entry to enum ${describe(enum)}")
        }

        /**
         * Adds an overload of a function in the same package, which differs only in a parameter of a more specific type:
         * `Int` instead of `Any`, or a non-nullable type instead of a nullable one. The existing calls pass a value of the more
         * specific type, so they resolve to the new overload, while their sources and the callee stay unchanged.
         */
        fun addSpecificOverload(): EditOutcome? {
            // a nullable and a non-nullable reference type have the same JVM signature, unlike a nullable and a non-nullable primitive type
            fun Param.isWidened() = type == ANY_TYPE || type.nullable && (type as? TypeRef.BuiltinType)?.builtin in PRIMITIVES
            val original = choices.pick(funs.filter { decl ->
                decl.params.none(Param::hasDefault) && decl.params.any { it.isWidened() } &&
                        funs.count { it.name == decl.name && it.arity == decl.arity } == 1
            }) ?: return null
            val file = choices.pick(program.files.filter { it.pkg == program.fileOf(original.id)?.pkg }) ?: return null
            val index = choices.pick(original.params.indices.filter { original.params[it].isWidened() })!!
            val narrowed = original.params[index].type.let { if (it == ANY_TYPE) TypeRef.BuiltinType(Builtin.INT) else it.withNullable(false) }
            val id = newId()
            // an inline overload could create an inline cycle, as the earlier functions may already call this name;
            // the overload is suspend and has a lambda as the original is printed, since the existing calls switch to it
            val overload = original.copy(
                id = id, inline = false, suspend = original.isSuspend,
                params = original.params.mapIndexed { i, param -> if (i == index) param.copy(type = narrowed) else param },
                returnType = randomType(), bodyVariant = choices.next(100), uses = emptyList(),
                deadBranchVariant = 0, ownedObjectVariant = 0, implicitReturnType = false,
            )
            addDecl(file, overload)
            return outcome("add overload ${overload.name} of ${describe(original)} with a parameter of type $narrowed to ${file.name}")
        }

        fun addInterface(): EditOutcome? {
            val [cls, added] = choices.pick(classes.flatMap { cls ->
                interfaceCandidates(cls.id, cls.superId).filter { it.id !in cls.interfaceIds }.map { cls to it }
            }) ?: return null
            val overrides = if (cls.kind.allowsAbstractMembers) emptyList() else {
                unimplementedAbstractMembers(listOf(added.id)).filter { member -> cls.members.none { it.overrides == member.id } }.map(::overrideOf)
            }
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(interfaceIds = it.interfaceIds + added.id, members = it.members + overrides) }
            return outcome("make ${describe(cls)} implement ${added.name}")
        }

        /**
         * Adds a function with the name and the signature of a function of another package to a package which does not declare it:
         * the files of that package which import the other package with a star import call the new function instead.
         */
        fun addShadowingFun(): EditOutcome? {
            val [shadowed, file] = choices.pick(funs.flatMap { decl ->
                val pkg = program.fileOf(decl.id)?.pkg
                program.files.filter { file -> file.pkg != pkg && funs.none { it.name == decl.name && program.fileOf(it.id)?.pkg == file.pkg } }
                    .map { decl to it }
            }) ?: return null
            val id = newId()
            val decl = shadowed.copy(
                id = id, inline = false, suspend = shadowed.isSuspend, bodyVariant = choices.next(100), uses = emptyList(),
                deadBranchVariant = 0, ownedObjectVariant = 0, implicitReturnType = false, deprecationConstId = null,
            )
            addDecl(file, decl)
            return outcome("add ${decl.name} shadowing ${describe(shadowed)} to ${file.name} in package ${file.pkg}")
        }

        /** Adds a member with the name and the signature of an extension function to its receiver, so that its calls call the member. */
        fun addShadowingMember(): EditOutcome? {
            val extension = choices.pick(funs.filter { it.receiverClassId != null && it.lambda == null && program.classDecl(it.receiverClassId) != null }) ?: return null
            val cls = program.classDecl(extension.receiverClassId!!)!!
            if (cls.members.any { it.name == extension.name }) return null
            val member = MemberFun(
                id = newId(), name = extension.name, modality = Modality.FINAL, overrides = null, returnType = extension.returnType,
                bodyVariant = choices.next(100), uses = emptyList(), params = extension.params,
            )
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(members = it.members + member) }
            return outcome("add member ${member.name} shadowing the extension ${describe(extension)} to ${describe(cls)}")
        }

        // ---- modifications of functions ----

        /** A top-level function or a member function, as a pair of the declaration id and the member id. */
        private fun pickFunction(includeOverrides: Boolean = true): Pair<Decl, MemberFun?>? = choices.pick(
            funs.map { it to null } + classes.flatMap { cls ->
                cls.members.filter { includeOverrides || it.overrides == null }.map { cls to it }
            }
        )

        private fun updateFunction(target: Pair<Decl, MemberFun?>, onFun: (FunDecl) -> FunDecl, onMember: (MemberFun) -> MemberFun) {
            val [decl, member] = target
            program = program.replaceDecl(decl.id) {
                if (member == null) onFun(it as FunDecl) else (it as ClassDecl).replaceMember(member.id, onMember)
            }
        }

        private fun describe(target: Pair<Decl, MemberFun?>): String =
            target.second?.let { "${target.first.name}.${it.name} in ${program.fileOf(target.first.id)?.name}" } ?: describe(target.first)

        fun changeFunBody(): EditOutcome? {
            val target = pickFunction() ?: return null
            updateFunction(target, { it.copy(bodyVariant = it.bodyVariant + 1) }, { it.copy(bodyVariant = it.bodyVariant + 1) })
            val inline = (target.first as? FunDecl)?.inline == true
            return outcome("change the body of ${if (inline) "inline " else ""}${describe(target)}")
        }

        fun toggleInline(): EditOutcome? {
            val decl = choices.pick(funs) ?: return null
            program = program.replaceDecl(decl.id) { (it as FunDecl).copy(inline = !it.inline) }
            return outcome("make ${describe(decl)} ${if (decl.inline) "non-inline" else "inline"}")
        }

        fun toggleSuspend(): EditOutcome? {
            val decl = choices.pick(funs) ?: return null
            program = program.replaceDecl(decl.id) { (it as FunDecl).copy(suspend = !it.suspend) }
            return outcome("make ${describe(decl)} ${if (decl.isSuspend) "non-suspend" else "suspend"}")
        }

        fun changeLambdaParam(): EditOutcome? {
            val decl = choices.pick(funs) ?: return null
            val lambda = choices.pick(LambdaKind.entries + null - decl.lambda)
            program = program.replaceDecl(decl.id) { (it as FunDecl).copy(lambda = lambda) }
            return outcome("change the lambda parameter of ${modifiers(decl)}${describe(decl)} to ${lambda?.name?.lowercase() ?: "none"}")
        }

        fun changeDeadBranch(): EditOutcome? {
            val decl = updateFun { decl ->
                if (decl.inline && decl.isSuspend && !KnownIssues.suspendInlineForInlineTemplate) return@updateFun null
                decl.copy(deadBranchVariant = decl.deadBranchVariant + 1)
            } ?: return null
            return outcome("change the dead branch of ${modifiers(decl)}${describe(decl)}")
        }

        fun changeOwnedObject(): EditOutcome? {
            // an anonymous object which captures nothing is not copied to the call sites of an inline function
            val decl = updateFun { it.copy(ownedObjectVariant = it.ownedObjectVariant + 1) } ?: return null
            return outcome("change the anonymous object in ${modifiers(decl)}${describe(decl)}")
        }

        fun changeReturnType(): EditOutcome? {
            val target = pickFunction(includeOverrides = false) ?: return null
            val newType = randomType()
            updateFunction(target, { it.copy(returnType = newType) }, { it.copy(returnType = newType) })
            return outcome("change the return type of ${describe(target)} to $newType")
        }

        fun toggleImplicitType(): EditOutcome? {
            val decl = choices.pick(funs + props) ?: return null
            val implicit = when (decl) {
                is FunDecl -> !decl.implicitReturnType
                is PropDecl -> !decl.implicitType
                else -> return null
            }
            program = program.replaceDecl(decl.id) {
                when (it) {
                    is FunDecl -> it.copy(implicitReturnType = implicit)
                    is PropDecl -> it.copy(implicitType = implicit)
                    else -> it
                }
            }
            return outcome("make the type of ${describe(decl)} ${if (implicit) "inferred" else "explicit"}")
        }

        private fun updateFun(transform: (FunDecl) -> FunDecl?): FunDecl? {
            val decl = choices.pick(funs) ?: return null
            val updated = transform(decl) ?: return null
            program = program.replaceDecl(decl.id) { updated }
            return decl
        }

        fun addParam(): EditOutcome? {
            val decl = updateFun {
                val name = "a${it.params.size}"
                it.copy(params = it.params + if (it.typeParam != null && choices.chance(30)) typeParamParam(name) else Param(name, randomType(), choices.chance(80)))
            } ?: return null
            return outcome("add a parameter to ${describe(decl)}")
        }

        /** Makes a function generic, changes the kind of its type parameter, or removes it, leaving its parameters of the type parameter unresolved. */
        fun changeTypeParam(): EditOutcome? {
            val decl = updateFun { it.copy(typeParam = choices.pick(TypeParamKind.entries + null - it.typeParam)) } ?: return null
            return outcome("change the type parameter of ${describe(decl)}")
        }

        fun removeParam(): EditOutcome? {
            val decl = updateFun { decl -> decl.params.takeIf { it.isNotEmpty() }?.let { decl.copy(params = it.dropLast(1)) } } ?: return null
            return outcome("remove the last parameter of ${describe(decl)}")
        }

        fun toggleParamDefault(): EditOutcome? {
            val decl = updateFun { decl ->
                if (decl.params.isEmpty()) return@updateFun null
                val index = choices.next(decl.params.size)
                decl.copy(params = decl.params.mapIndexed { i, param -> if (i == index) param.copy(hasDefault = !param.hasDefault) else param })
            } ?: return null
            return outcome("toggle a parameter default value of ${describe(decl)}")
        }

        fun changeParamDefaultValue(): EditOutcome? {
            val decl = updateFun { decl ->
                if (decl.inline && !KnownIssues.inlineDefaultValueChange) return@updateFun null
                val index = choices.pick(decl.params.indices.filter { decl.params[it].hasDefault }) ?: return@updateFun null
                decl.copy(params = decl.params.mapIndexed { i, param -> if (i == index) param.copy(defaultVariant = param.defaultVariant + 1) else param })
            } ?: return null
            return outcome("change a parameter default value of ${if (decl.inline) "inline " else ""}${describe(decl)}")
        }

        fun changeParamType(): EditOutcome? {
            val decl = updateFun { decl ->
                if (decl.params.isEmpty()) return@updateFun null
                val index = choices.next(decl.params.size)
                decl.copy(params = decl.params.mapIndexed { i, param -> if (i == index) param.copy(type = randomType()) else param })
            } ?: return null
            return outcome("change a parameter type of ${describe(decl)}")
        }

        /** Renames a parameter; the existing calls pass the argument by the old name, so they stop compiling. */
        fun renameParam(): EditOutcome? {
            val decl = updateFun { decl ->
                if (decl.params.isEmpty()) return@updateFun null
                val index = choices.next(decl.params.size)
                decl.copy(params = decl.params.mapIndexed { i, param -> if (i == index) param.copy(name = "b${newId()}") else param })
            } ?: return null
            return outcome("rename a parameter of ${describe(decl)}")
        }

        fun changeCallLambda(): EditOutcome? {
            val [target, index] = choices.pick(
                (funs.map { it to null } + classes.flatMap { cls -> cls.members.map { cls to it } }).flatMap { target ->
                    val uses = target.second?.uses ?: (target.first as FunDecl).uses
                    uses.indices.filter { uses[it] is Use.CallFun }.map { target to it }
                }
            ) ?: return null
            fun List<Use>.changed() = mapIndexed { i, use -> if (i == index && use is Use.CallFun) use.copy(lambdaVariant = use.lambdaVariant + 1) else use }
            updateFunction(target, { it.copy(uses = it.uses.changed()) }, { it.copy(uses = it.uses.changed()) })
            return outcome("change a lambda passed by ${describe(target)}")
        }

        fun toggleDeprecationConst(): EditOutcome? {
            val decl = choices.pick(funs) ?: return null
            val constId = if (decl.deprecationConstId != null) null else choices.pick(props.filter { it.kind == PropKind.CONST })?.id ?: return null
            program = program.replaceDecl(decl.id) { (it as FunDecl).copy(deprecationConstId = constId) }
            return outcome("${if (constId == null) "remove the deprecation of" else "deprecate"} ${describe(decl)}")
        }

        fun changeReceiver(): EditOutcome? {
            val decl = choices.pick(funs) ?: return null
            val receiver = choices.pick(classes.map { it.id } + null - decl.receiverClassId)
            program = program.replaceDecl(decl.id) { (it as FunDecl).copy(receiverClassId = receiver) }
            return outcome("change the receiver of ${describe(decl)} to ${receiver?.let { program.decl(it)?.name } ?: "none"}")
        }

        fun toggleFun(what: String, transform: (FunDecl) -> FunDecl): EditOutcome? {
            val decl = updateFun(transform) ?: return null
            return outcome("toggle $what of ${describe(decl)}")
        }

        fun changeFunJvmName(): EditOutcome? {
            val decl = updateFun { it.copy(jvmNameVariant = nextVariant(it.jvmNameVariant)) } ?: return null
            return outcome("change the JVM name of ${describe(decl)}")
        }

        fun toggleInfix(): EditOutcome? {
            val decl = choices.pick(funs.filter { it.isBinary }) ?: return null
            program = program.replaceDecl(decl.id) { (it as FunDecl).copy(infix = !it.infix) }
            return outcome("make ${describe(decl)} ${if (decl.infix) "non-infix" else "infix"}")
        }

        /** Makes a function the `plus` operator, renaming it, or makes it a regular function keeping the name. */
        fun toggleOperator(): EditOutcome? {
            val decl = choices.pick(funs.filter { it.isBinary }) ?: return null
            program = program.replaceDecl(decl.id) { (it as FunDecl).copy(operator = !it.operator, name = if (it.operator) it.name else "plus") }
            return outcome("make ${describe(decl)} ${if (decl.operator) "a regular function" else "the plus operator"}")
        }

        // ---- modifications of other declarations ----

        fun changeVisibility(): EditOutcome? {
            val decl = choices.pick(program.decls) ?: return null
            val visibility = choices.pick(Visibility.TOP_LEVEL - decl.visibility)!!
            program = program.replaceDecl(decl.id) { it.withVisibility(visibility) }
            return outcome("make ${describe(decl)} ${visibility.name.lowercase()}")
        }

        fun changePropValue(): EditOutcome? {
            val decl = choices.pick(props) ?: return null
            program = program.replaceDecl(decl.id) { (it as PropDecl).copy(valueVariant = it.valueVariant + 1) }
            return outcome("change the value of ${decl.kind.name.lowercase()} ${describe(decl)}")
        }

        fun changePropKind(): EditOutcome? {
            val decl = choices.pick(props) ?: return null
            val kind = choices.pick(PropKind.entries - decl.kind)!!
            program = program.replaceDecl(decl.id) { (it as PropDecl).copy(kind = kind) }
            return outcome("make ${describe(decl)} ${kind.name.lowercase()}")
        }

        fun changePropType(): EditOutcome? {
            val decl = choices.pick(props) ?: return null
            val type = randomType()
            program = program.replaceDecl(decl.id) { (it as PropDecl).copy(type = type) }
            return outcome("change the type of ${describe(decl)} to $type")
        }

        fun changePropAccessor(): EditOutcome? {
            val decl = choices.pick(props) ?: return null
            val accessor = choices.pick(PropAccessor.entries - decl.accessor)!!
            program = program.replaceDecl(decl.id) { (it as PropDecl).copy(accessor = accessor) }
            return outcome("change the accessor of ${describe(decl)} to ${accessor.name.lowercase()}")
        }

        fun changePropReceiver(): EditOutcome? {
            val decl = choices.pick(props) ?: return null
            val receiver = choices.pick(classes.map { it.id } + null - decl.receiverClassId)
            program = program.replaceDecl(decl.id) { (it as PropDecl).copy(receiverClassId = receiver) }
            return outcome("change the receiver of ${describe(decl)} to ${receiver?.let { program.decl(it)?.name } ?: "none"}")
        }

        fun toggleJvmField(): EditOutcome? {
            val decl = choices.pick(props) ?: return null
            program = program.replaceDecl(decl.id) { (it as PropDecl).copy(jvmField = !it.jvmField) }
            return outcome("toggle @JvmField of ${describe(decl)}")
        }

        fun toggleVolatile(): EditOutcome? {
            val decl = choices.pick(props) ?: return null
            program = program.replaceDecl(decl.id) { (it as PropDecl).copy(volatile = !it.volatile) }
            return outcome("toggle @Volatile of ${describe(decl)}")
        }

        fun changeAliasTarget(): EditOutcome? {
            val decl = choices.pick(aliases) ?: return null
            val target = randomType(maxId = decl.id)
            program = program.replaceDecl(decl.id) { (it as TypeAliasDecl).copy(target = target) }
            return outcome("change the target of typealias ${describe(decl)} to $target")
        }

        fun changeClassKind(): EditOutcome? {
            val cls = choices.pick(classes) ?: return null
            val kind = choices.pick(ClassKind.entries - cls.kind)!!
            program = program.replaceDecl(cls.id) {
                (it as ClassDecl).copy(kind = kind, enumEntries = if (kind == ClassKind.ENUM && it.enumEntries.isEmpty()) listOf("E0") else it.enumEntries)
            }
            return outcome("make ${describe(cls)} ${kind.name.lowercase()}")
        }

        fun changeSuper(): EditOutcome? {
            val cls = choices.pick(classes) ?: return null
            val newSuper = choices.pick(superCandidates(cls.kind, cls.id).filter { it.id != cls.superId })
            if (newSuper == null && cls.superId == null) return null
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(superId = newSuper?.id) }
            return outcome("change the supertype of ${describe(cls)} to ${newSuper?.name ?: "none"}")
        }

        fun removeInterface(): EditOutcome? {
            val cls = choices.pick(classes.filter { it.interfaceIds.isNotEmpty() }) ?: return null
            val removed = choices.pick(cls.interfaceIds)!!
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(interfaceIds = it.interfaceIds - removed) }
            return outcome("remove the interface ${program.decl(removed)?.name} from the supertypes of ${describe(cls)}")
        }

        fun reorderSupertypes(): EditOutcome? {
            val cls = choices.pick(classes.filter { it.interfaceIds.size > 1 }) ?: return null
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(interfaceIds = it.interfaceIds.reversed()) }
            return outcome("reorder the interfaces of ${describe(cls)}")
        }

        fun toggleInner(): EditOutcome? {
            val cls = choices.pick(classes.filter { it.outerId != null && it.nesting != Nesting.COMPANION }) ?: return null
            val nesting = if (cls.nesting == Nesting.INNER) Nesting.NESTED else Nesting.INNER
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(nesting = nesting) }
            return outcome("make ${describe(cls)} ${nesting.name.lowercase()}")
        }

        fun moveNestedOut(): EditOutcome? {
            val cls = choices.pick(classes.filter { it.outerId != null }) ?: return null
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(outerId = null) }
            return outcome("move the nested ${describe(cls)} out of its outer class")
        }

        fun toggleJvmStatic(): EditOutcome? {
            val [cls, member] = choices.pick(classes.filter { it.kind == ClassKind.OBJECT }.flatMap { cls ->
                cls.members.filter { it.overrides == null && it.modality == Modality.FINAL }.map { cls to it }
            }) ?: return null
            updateMember(cls, member) { it.copy(jvmStatic = !it.jvmStatic) }
            return outcome("make ${cls.name}.${member.name} in ${program.fileOf(cls.id)?.name} ${if (member.jvmStatic) "non-static" else "@JvmStatic"}")
        }

        fun nextVariant(variant: Int) = if (variant != 0 && choices.chance(30)) 0 else variant + 1

        fun changeEnumConstructor(): EditOutcome? {
            val cls = choices.pick(classes.filter { it.kind == ClassKind.ENUM }) ?: return null
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(enumConstructorVariant = nextVariant(it.enumConstructorVariant)) }
            return outcome("change the constructor arguments of the enum entries of ${describe(cls)}")
        }

        fun changeEnumEntryBodies(): EditOutcome? {
            val cls = choices.pick(classes.filter { it.kind == ClassKind.ENUM }) ?: return null
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(enumEntryBodyVariant = nextVariant(it.enumEntryBodyVariant)) }
            return outcome("change the bodies of the enum entries of ${describe(cls)}")
        }

        fun toggleFunInterface(): EditOutcome? {
            val cls = choices.pick(classes.filter { it.kind == ClassKind.INTERFACE }) ?: return null
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(funInterface = !it.funInterface) }
            return outcome("make ${describe(cls)} ${if (cls.funInterface) "a regular" else "a fun"} interface")
        }

        /** Renames an enum entry; the existing `when` uses keep the old name, so they stop compiling. */
        fun renameEnumEntry(): EditOutcome? {
            val enum = choices.pick(classes.filter { it.kind == ClassKind.ENUM && it.enumEntries.isNotEmpty() }) ?: return null
            val entry = choices.pick(enum.enumEntries)!!
            val newName = "E${newId()}"
            program = program.replaceDecl(enum.id) { (it as ClassDecl).copy(enumEntries = it.enumEntries.map { e -> if (e == entry) newName else e }) }
            return outcome("rename the entry $entry of enum ${describe(enum)} to $newName")
        }

        fun changeMemberModality(): EditOutcome? {
            val [cls, member] = choices.pick(classes.flatMap { cls -> cls.members.filter { it.overrides == null }.map { cls to it } }) ?: return null
            val modality = choices.pick(Modality.entries - member.modality)!!
            program = program.replaceDecl(cls.id) { (it as ClassDecl).replaceMember(member.id) { m -> m.copy(modality = modality) } }
            return outcome("make ${cls.name}.${member.name} ${modality.name.lowercase()} in ${program.fileOf(cls.id)?.name}")
        }

        /** A member which is not an override, as a pair of its class and the member. */
        private fun pickRootMember(filter: (MemberFun) -> Boolean = { true }): Pair<ClassDecl, MemberFun>? =
            choices.pick(classes.flatMap { cls -> cls.members.filter { it.overrides == null && filter(it) }.map { cls to it } })

        private fun updateMember(cls: ClassDecl, member: MemberFun, transform: (MemberFun) -> MemberFun) {
            program = program.replaceDecl(cls.id) { (it as ClassDecl).replaceMember(member.id, transform) }
        }

        private fun updatePickedMember(what: String, picked: Pair<ClassDecl, MemberFun>?, transform: (MemberFun) -> MemberFun): EditOutcome? {
            val [cls, member] = picked ?: return null
            updateMember(cls, member, transform)
            return outcome("toggle $what of ${cls.name}.${member.name} in ${program.fileOf(cls.id)?.name}")
        }

        fun updateRootMember(what: String, transform: (MemberFun) -> MemberFun): EditOutcome? =
            updatePickedMember(what, pickRootMember { it.isProperty }, transform)

        fun updateAnyMember(what: String, transform: (MemberFun) -> MemberFun): EditOutcome? =
            updatePickedMember(what, choices.pick(classes.flatMap { cls -> cls.members.map { cls to it } }), transform)

        fun updateOverride(what: String, transform: (MemberFun) -> MemberFun): EditOutcome? =
            updatePickedMember(what, choices.pick(classes.flatMap { cls -> cls.members.filter { it.overrides != null }.map { cls to it } }), transform)

        fun addMemberParam(): EditOutcome? {
            val [cls, member] = pickRootMember { !it.isProperty } ?: return null
            updateMember(cls, member) { it.copy(params = it.params + Param("a${it.params.size}", randomType(), choices.chance(80))) }
            return outcome("add a parameter to ${cls.name}.${member.name} in ${program.fileOf(cls.id)?.name}")
        }

        fun toggleMemberProperty(): EditOutcome? {
            val [cls, member] = pickRootMember { it.isProperty || it.params.isEmpty() } ?: return null
            updateMember(cls, member) { it.copy(isProperty = !it.isProperty) }
            return outcome("make ${cls.name}.${member.name} in ${program.fileOf(cls.id)?.name} a ${if (member.isProperty) "function" else "property"}")
        }

        fun changeMemberVisibility(): EditOutcome? {
            val [cls, member] = pickRootMember() ?: return null
            val visibility = choices.pick(Visibility.entries - member.visibility)!!
            updateMember(cls, member) { it.copy(visibility = visibility) }
            return outcome("make ${cls.name}.${member.name} in ${program.fileOf(cls.id)?.name} ${visibility.name.lowercase()}")
        }

        fun toggleCovariantReturn(): EditOutcome? {
            val [cls, member] = choices.pick(classes.flatMap { cls -> cls.members.filter { it.overrides != null }.map { cls to it } }) ?: return null
            updateMember(cls, member) { it.copy(covariantReturnType = !it.covariantReturnType) }
            return outcome("make the return type of the override ${cls.name}.${member.name} in ${program.fileOf(cls.id)?.name} ${if (member.covariantReturnType) "the overridden one" else "non-nullable"}")
        }

        // ---- removals ----

        fun removeMember(): EditOutcome? {
            val [cls, member] = choices.pick(classes.flatMap { cls -> cls.members.map { cls to it } }) ?: return null
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(members = it.members - member) }
            return outcome("remove ${cls.name}.${member.name} from ${program.fileOf(cls.id)?.name}")
        }

        fun removeUse(): EditOutcome? {
            val target = choices.pick(
                funs.filter { it.uses.isNotEmpty() }.map { it to null } +
                        classes.flatMap { cls -> cls.members.filter { it.uses.isNotEmpty() }.map { cls to it } }
            ) ?: return null
            val uses = target.second?.uses ?: (target.first as FunDecl).uses
            val index = choices.next(uses.size)
            updateFunction(
                target,
                { it.copy(uses = it.uses.filterIndexed { i, _ -> i != index }) },
                { it.copy(uses = it.uses.filterIndexed { i, _ -> i != index }) },
            )
            return outcome("remove use ${uses[index]} from ${describe(target)}")
        }

        fun removeEnumEntry(): EditOutcome? {
            val enum = choices.pick(classes.filter { it.kind == ClassKind.ENUM && it.enumEntries.isNotEmpty() }) ?: return null
            val entry = choices.pick(enum.enumEntries)!!
            program = program.replaceDecl(enum.id) { (it as ClassDecl).copy(enumEntries = it.enumEntries - entry) }
            return outcome("remove entry $entry from enum ${describe(enum)}")
        }

        fun removeDecl(): EditOutcome? {
            val decl = choices.pick(program.decls) ?: return null
            val description = "remove ${describe(decl)}"
            program = program.copy(files = program.files.map { file -> file.copy(decls = file.decls.filter { it.id != decl.id }) })
            return outcome(description)
        }

        fun renameDecl(): EditOutcome? {
            val decl = choices.pick(program.decls) ?: return null
            val newName = "${decl.name.takeWhile { it.isLetter() }}${newId()}"
            val description = "rename ${describe(decl)} to $newName"
            program = program.replaceDecl(decl.id) { it.withName(newName) }
            return outcome(description)
        }

        fun moveDecl(): EditOutcome? {
            val decl = choices.pick(program.decls) ?: return null
            val from = program.fileOf(decl.id)!!
            val to = choices.pick(program.files - from) ?: return null
            program = program.copy(files = program.files.map { file ->
                when (file.name) {
                    from.name -> file.copy(decls = file.decls - decl)
                    to.name -> file.copy(decls = file.decls + decl)
                    else -> file
                }
            })
            return outcome("move ${decl.name} from ${from.name} (${from.pkg}) to ${to.name} (${to.pkg})")
        }

        // ---- file-level edits ----

        fun changePackage(): EditOutcome? {
            val file = pickFile() ?: return null
            val pkg = choices.pick(PACKAGES - file.pkg)!!
            program = program.replaceFile(file.name) { it.copy(pkg = pkg) }
            return outcome("change the package of ${file.name} to $pkg")
        }

        fun renameFile(): EditOutcome? {
            val file = pickFile() ?: return null
            val newName = "f${newId()}.kt"
            program = program.replaceFile(file.name) { it.copy(name = newName) }
            return outcome("rename ${file.name} to $newName")
        }

        fun deleteFile(): EditOutcome? {
            val file = pickFile() ?: return null
            program = program.copy(files = program.files - file)
            return outcome("delete ${file.name} with ${file.decls.joinToString { it.name }.ifEmpty { "no declarations" }}")
        }

        fun touchFile(): EditOutcome? {
            val file = pickFile() ?: return null
            return outcome("touch ${file.name}", touchedFiles = setOf(file.name))
        }

        fun changeComment(): EditOutcome? {
            val file = pickFile() ?: return null
            program = program.replaceFile(file.name) { it.copy(commentVariant = it.commentVariant + 1) }
            return outcome("change a comment in ${file.name}")
        }

        fun shiftLines(): EditOutcome? {
            val file = choices.pick(program.files.filter { it.decls.isNotEmpty() }) ?: return null
            val decl = choices.pick(file.decls)!!
            program = program.replaceFile(file.name) { it.copy(blankLines = it.blankLines + (decl.id to (it.blankLines[decl.id] ?: 0) + 1)) }
            return outcome("add a blank line before ${describe(decl)}")
        }

        fun reorderDecls(): EditOutcome? {
            val file = choices.pick(program.files.filter { it.decls.size > 1 }) ?: return null
            val moved = choices.pick(file.decls)!!
            val rest = file.decls - moved
            val index = choices.next(rest.size + 1)
            program = program.replaceFile(file.name) { it.copy(decls = rest.take(index) + moved + rest.drop(index)) }
            return outcome("move ${describe(moved)} to the position $index")
        }

        fun reorderMembers(): EditOutcome? {
            val cls = choices.pick(classes.filter { it.members.size > 1 }) ?: return null
            program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(members = it.members.drop(1) + it.members.first()) }
            return outcome("move the first member of ${describe(cls)} to the end")
        }

        fun toggleStarImports(): EditOutcome? {
            val file = pickFile() ?: return null
            program = program.replaceFile(file.name) { it.copy(starImports = !it.starImports) }
            return outcome("use ${if (file.starImports) "explicit" else "star"} imports in ${file.name}")
        }

        fun toggleImportAlias(): EditOutcome? {
            val file = pickFile() ?: return null
            val decl = choices.pick(program.decls.filter { program.fileOf(it.id)?.pkg != file.pkg }) ?: return null
            val aliased = decl.id !in file.aliasedIds
            program = program.replaceFile(file.name) { it.copy(aliasedIds = if (aliased) it.aliasedIds + decl.id else it.aliasedIds - decl.id) }
            return outcome("${if (aliased) "import ${decl.name} with" else "import ${decl.name} without"} an alias in ${file.name}")
        }

        fun changeFileJvmName(): EditOutcome? {
            val file = pickFile() ?: return null
            program = program.replaceFile(file.name) { it.copy(jvmNameVariant = if (it.jvmNameVariant != 0 && choices.chance(30)) 0 else choices.next(3) + 1) }
            return outcome("change the JVM name of the facade of ${file.name}")
        }

        fun toggleMultifile(): EditOutcome? {
            val file = pickFile() ?: return null
            program = program.replaceFile(file.name) { it.copy(multifile = !it.multifile) }
            return outcome("toggle @JvmMultifileClass of ${file.name}")
        }

        fun complete(): Program {
            // a SAM conversion stops compiling when a later edit adds an abstract member to its interface
            fun Use.refreshed(): Use? = when (this) {
                is Use.WhenSealed -> copy(branchClassIds = program.directSubclasses(sealedId).map { it.id })
                is Use.WhenEnum -> copy(entries = program.classDecl(enumId)?.enumEntries ?: entries)
                is Use.SamConversion -> takeIf { program.classDecl(interfaceId)?.samMember != null }
                else -> this
            }
            for (decl in program.decls) {
                program = program.replaceDecl(decl.id) {
                    when (it) {
                        is FunDecl -> it.copy(uses = it.uses.mapNotNull { use -> use.refreshed() })
                        is ClassDecl -> it.copy(members = it.members.map { member -> member.copy(uses = member.uses.mapNotNull { use -> use.refreshed() }) })
                        else -> it
                    }
                }
            }
            // supertypes have smaller ids, so they are completed before their subclasses
            for (cls in classes.sortedBy { it.id }) {
                if (cls.kind.allowsAbstractMembers) continue
                val missing = unimplementedAbstractMembers(listOf(cls.id))
                if (missing.isEmpty()) continue
                val overrides = missing.map(::overrideOf)
                program = program.replaceDecl(cls.id) { (it as ClassDecl).copy(members = it.members + overrides) }
            }
            return program
        }
    }
}
