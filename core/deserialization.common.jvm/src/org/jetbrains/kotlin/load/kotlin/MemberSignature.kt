/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature

// The purpose of this class is to hold a unique signature of either a method or a field, so that annotations on a member can be put
// into a map indexed by these signatures
@Suppress("DataClassPrivateConstructor")
data class MemberSignature private constructor(val signature: String) {
    companion object {
        @JvmStatic
        fun fromMethod(nameResolver: NameResolver, signature: JvmProtoBuf.JvmMethodSignature): MemberSignature {
            return fromMethodNameAndDesc(nameResolver.getString(signature.name), nameResolver.getString(signature.desc))
        }

        @JvmStatic
        fun fromMethodNameAndDesc(name: String, desc: String): MemberSignature {
            return MemberSignature(name + desc)
        }

        @JvmStatic
        fun fromFieldNameAndDesc(name: String, desc: String): MemberSignature {
            return MemberSignature("$name#$desc")
        }

        @JvmStatic
        fun fromJvmMemberSignature(signature: JvmMemberSignature): MemberSignature = when (signature) {
            is JvmMemberSignature.Method -> fromMethodNameAndDesc(signature.name, signature.desc)
            is JvmMemberSignature.Field -> fromFieldNameAndDesc(signature.name, signature.desc)
        }

        @JvmStatic
        fun fromMethodSignatureAndParameterIndex(signature: MemberSignature, index: Int): MemberSignature {
            return MemberSignature("${signature.signature}@$index")
        }
    }
}
