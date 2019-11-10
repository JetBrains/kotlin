/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util

import org.jetbrains.kotlin.decompiler.util.name
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

private val defaultImportRegex = setOf(
    "kotlin\\.\\w+",
    "kotlin.annotation\\.\\w+",
    "kotlin.collections\\.\\w+",
    "kotlin.comparisons\\.\\w+",
    "kotlin.io\\.\\w+",
    "kotlin.ranges\\.\\w+",
    "kotlin.sequences\\.\\w+",
    "kotlin.text\\.\\w+"
).map { it.toRegex() }

class ImportResolveVisitor(val importDirectivesSet: MutableSet<String> = mutableSetOf<String>()) : IrElementVisitor<Unit, String> {
    private val declaredNamesSet = mutableSetOf<String>()
    private val calledNamesSet = mutableSetOf<String>()

    companion object {
        private fun MutableSet<String>.add(nullable: String?) {
            if (nullable != null) add(nullable)
        }

        fun IrType.asImportStr() =
            (this as? IrSimpleType)?.abbreviation?.typeAlias?.owner?.fqNameWhenAvailable?.asString()
                ?: getClass()?.asImportStr()

        fun IrClass.asImportStr() = fqNameWhenAvailable?.asString()?.takeIf { !isLocalClass() }
    }

    override fun visitFile(declaration: IrFile, data: String) {
        with(declaration) {
            val packageName = fqName.asString().takeIf { fqName.asString().isNotEmpty() } ?: EMPTY_TOKEN
            declarations
                .forEach { it.accept(this@ImportResolveVisitor, packageName) }

        }
        importDirectivesSet.addAll(calledNamesSet)
        importDirectivesSet.removeAll(declaredNamesSet)
        defaultImportRegex.forEach { regex ->
            importDirectivesSet.removeIf { it.matches(regex) }
        }
    }

    /**
     * Class definition processing includes adding a name to the collection of declarations
     * and sequential processing of supertypes and all members of the class.
     */
    override fun visitClass(declaration: IrClass, data: String) {
        with(declaration) {
            val declarationNameWithPrefix = "${("$data.").takeIf { data.isNotEmpty() } ?: EMPTY_TOKEN}${this.name.identifier}"
            declaredNamesSet.add(declarationNameWithPrefix)
            // Для енамов в суперах лежит Enum<MyType>, который почему-то не isEnum
            // Добавляем
            superTypes.filterNot { it.isAny() || it.toKotlinType().toString().startsWith("Enum") }
                .forEach { calledNamesSet.add(it.asImportStr()) }
            declarations.filterNot {
                it.origin in setOf(
                    IrDeclarationOrigin.FAKE_OVERRIDE,
                    IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER,
                    IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER
                )
            }
                .forEach { it.accept(this@ImportResolveVisitor, declarationNameWithPrefix) }
        }
    }

    override fun visitProperty(declaration: IrProperty, data: String) {
        with(declaration) {
            val declarationNameWithPrefix = "$data.${name()}"
            if (parent is IrFile || parentAsClass.isCompanion || parentAsClass.isObject) {
                declaredNamesSet.add(declarationNameWithPrefix)
            }
            backingField?.initializer?.accept(this@ImportResolveVisitor, declarationNameWithPrefix)
        }
    }

    override fun visitConstructor(declaration: IrConstructor, data: String) {
        with(declaration) {
            calledNamesSet.add(parentAsClass.asImportStr())
            typeParameters.forEach { it.accept(this@ImportResolveVisitor, data) }
            valueParameters.forEach { it.accept(this@ImportResolveVisitor, data) }
            body?.accept(this@ImportResolveVisitor, data)
        }
        run { }
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: String) {
        with(expression) {
            calledNamesSet.add(symbol.owner.parentAsClass.asImportStr())
            (0 until typeArgumentsCount).forEach { calledNamesSet.add(getTypeArgument(it)?.asImportStr()) }
            (0 until valueArgumentsCount).forEach { getValueArgument(it)?.accept(this@ImportResolveVisitor, data) }
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: String) {
        calledNamesSet.add(expression.symbol.owner.parentAsClass.asImportStr())
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: String) {
        with(declaration) {
            val declarationNameWithPrefix = "$data.${name()}"
            declaredNamesSet.add(declarationNameWithPrefix)
            initializerExpression?.accept(this@ImportResolveVisitor, declarationNameWithPrefix)
        }
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: String) {
        // Тут кладем в импорты имя енама и обрабатываем все аргументы
        with(expression) {
            (0 until valueArgumentsCount).forEach { getValueArgument(it)?.accept(this@ImportResolveVisitor, data) }
        }
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: String) {
        val ownerEnumEntry = expression.symbol.owner
        with(ownerEnumEntry) {
            calledNamesSet.add(ownerEnumEntry.fqNameWhenAvailable?.asString() ?: name())
            initializerExpression?.accept(this@ImportResolveVisitor, "$data.${name()}")
        }
    }

    override fun visitBody(body: IrBody, data: String) {
        body.statements.forEach { it.accept(this, data) }
    }

    override fun visitValueParameter(declaration: IrValueParameter, data: String) {
        calledNamesSet.add(declaration.type.asImportStr())
    }

    override fun visitFunction(declaration: IrFunction, data: String) {
        with(declaration) {
            typeParameters.forEach { it.accept(this@ImportResolveVisitor, data) }
            valueParameters.forEach { it.accept(this@ImportResolveVisitor, data) }
            body?.accept(this@ImportResolveVisitor, data)
        }
    }

    override fun visitReturn(expression: IrReturn, data: String) {
        expression.value.accept(this, data)
    }

    override fun visitWhen(expression: IrWhen, data: String) {
        expression.branches.forEach { it.accept(this, data) }
    }

    override fun visitBranch(branch: IrBranch, data: String) {
        with(branch) {
            condition.accept(this@ImportResolveVisitor, data)
            result.accept(this@ImportResolveVisitor, data)
        }
    }

    override fun visitCall(expression: IrCall, data: String) {
        //TODO Здесь может быть больше всего проблем (extension и dispatcher receivers, superQualifier)
        with(expression) {
            dispatchReceiver?.accept(this@ImportResolveVisitor, data)
            (0 until typeArgumentsCount).forEach { calledNamesSet.add(getTypeArgument(it)?.asImportStr()) }
            (0 until valueArgumentsCount).forEach { getValueArgument(it)?.accept(this@ImportResolveVisitor, data) }

        }
    }

    override fun visitVariable(declaration: IrVariable, data: String) {
        //Здесь обрабатываем тип (потому что мог быть введен явно, а мы выводим явно всегда) и initializer
        with(declaration) {
            calledNamesSet.add(type.asImportStr())
            initializer?.accept(this@ImportResolveVisitor, data)
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: String) {
        with(expression) {
            val owner = symbol.owner
            if (!owner.isCompanion || owner.name() != "Companion") {
                //Здесь нужно учитывать локальные классы/методы?
                calledNamesSet.add(owner.asImportStr())
            } else {
                //Здесь проблема, если у companion object могут быть вложенные (но вроде не может)
                calledNamesSet.add(owner.parentAsClass.asImportStr())
            }
        }
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: String) {
        declaredNamesSet.add("$data.${declaration.name()}")
    }

    override fun visitElement(element: IrElement, data: String) {
        run { }
    }

}