/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.expressions

enum class IrCallableOperator {
    PROPERTY_GET,
    PROPERTY_SET,
    INVOKE,
    INVOKE_EXTENSION,
    PLUSPLUS,
    MINUSMINUS,
    UMINUS,
    EXCL,
    LT,
    GT,
    LTEQ,
    GTEQ,
    EQEQ,
    EQEQEQ,
    EXCLEQ,
    EXCLEQEQ,
    IN,
    NOT_IN,
    ANDAND,
    OROR,
    RANGE,
    PLUS,
    MINUS,
    MUL,
    DIV,
    MOD,
    PLUSEQ,
    MINUSEQ,
    MULEQ,
    DIVEQ,
    MODEQ;
}

fun IrCallableOperator.isCompoundAssignment(): Boolean =
        this >= IrCallableOperator.PLUSEQ && this <= IrCallableOperator.MODEQ

fun IrCallableOperator.hasCompoundAssignmentDual(): Boolean =
        this >= IrCallableOperator.PLUS && this <= IrCallableOperator.MOD

fun IrCallableOperator.toDualOperator(): IrCallableOperator =
        when {
            isCompoundAssignment() ->
                IrCallableOperator.values()[this.ordinal - IrCallableOperator.PLUSEQ.ordinal + IrCallableOperator.PLUS.ordinal]
            hasCompoundAssignmentDual() ->
                IrCallableOperator.values()[this.ordinal - IrCallableOperator.PLUS.ordinal + IrCallableOperator.PLUSEQ.ordinal]
            else ->
                throw UnsupportedOperationException("Operator $this is not a compound assignment")
        }


