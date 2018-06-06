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

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.name.Name

interface IrClass : IrSymbolDeclaration<IrClassSymbol>, IrDeclarationContainer, IrTypeParametersContainer {
    override val descriptor: ClassDescriptor

    val name: Name
    val kind: ClassKind
    val visibility: Visibility
    val modality: Modality
    val isCompanion: Boolean
    val isInner: Boolean
    val isData: Boolean
    val isExternal: Boolean

    // NB type parameters can't be top-level classifiers in supetypes of a class
    val superClasses: MutableList<IrClassSymbol>

    var thisReceiver: IrValueParameter?
}

fun IrClass.addMember(member: IrDeclaration) {
    declarations.add(member)
}

fun IrClass.addAll(members: List<IrDeclaration>) {
    declarations.addAll(members)
}

fun IrClass.getInstanceInitializerMembers() =
    declarations.filter {
        when (it) {
            is IrAnonymousInitializer ->
                true
            is IrProperty ->
                it.backingField?.initializer != null
            is IrField ->
                it.initializer != null
            else -> false
        }
    }

