package org.jetbrains.kotlin.config

import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType

interface KotlinRootType {
    fun isTestRoot(): Boolean
}

//Hack required to fix critical bug KT-27718 it should be removed after support of source root types other than JavaSourceRootType in Idea Core
fun isSameRootType(kotlinRoot: KotlinRootType, other: Any?): Boolean {
    if (kotlinRoot.isTestRoot()) {
        if (other is JavaResourceRootType || other is JavaSourceRootType) {
            val stack = Thread.currentThread().stackTrace
            if (stack.size > 2 && "isTestSource" == stack[2].methodName && "com.intellij.openapi.roots.impl.SourceFolderImpl" == stack[2].className) {
                if (JavaResourceRootType.TEST_RESOURCE == other) {
                    return true
                }
            }
        }
    }
    return false
}
