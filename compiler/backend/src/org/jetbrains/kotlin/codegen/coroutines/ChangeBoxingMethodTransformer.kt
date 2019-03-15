/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.optimization.boxing.isPrimitiveBoxing
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.codegen.topLevelClassInternalName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

private val BOXING_CLASS_INTERNAL_NAME =
    RELEASE_COROUTINES_VERSION_SETTINGS.coroutinesJvmInternalPackageFqName().child(Name.identifier("Boxing")).topLevelClassInternalName()

object ChangeBoxingMethodTransformer : MethodTransformer() {
    private val wrapperToInternalBoxing: Map<String, String>

    init {
        val map = hashMapOf<String, String>()
        for (primitiveType in JvmPrimitiveType.values()) {
            val name = primitiveType.wrapperFqName.topLevelClassInternalName()
            map[name] = "box${primitiveType.javaKeywordName.capitalize()}"
        }
        wrapperToInternalBoxing = map
    }

    override fun transform(internalClassName: String, methodNode: MethodNode) {
        for (boxing in methodNode.instructions.asSequence().filter { it.isPrimitiveBoxing() }) {
            assert(boxing.opcode == Opcodes.INVOKESTATIC) {
                "boxing shall be INVOKESTATIC wrapper.valueOf"
            }
            boxing as MethodInsnNode
            val methodName = wrapperToInternalBoxing[boxing.owner].sure {
                "expected primitive wrapper, but got ${boxing.owner}"
            }
            methodNode.instructions.set(
                boxing,
                MethodInsnNode(boxing.opcode, BOXING_CLASS_INTERNAL_NAME, methodName, boxing.desc, false)
            )
        }
    }
}
