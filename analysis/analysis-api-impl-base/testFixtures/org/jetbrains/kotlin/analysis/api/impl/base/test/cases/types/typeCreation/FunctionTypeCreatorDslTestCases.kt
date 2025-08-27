/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.plugin.sandbox.fir.types.PluginFunctionalNames

@Suppress("UNUSED")
class FunctionTypeCreatorDslTestCases(session: KaSession, caretToType: Map<String, KaType>) :
    AbstractTypeCreatorDslTest.TestCases(session, caretToType) {
    fun testBasicFunWithIntReturnType(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.functionType {
            returnType = type
        }
    }

    fun testBasicFunWithIntReturnTypeAndReceiver(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.functionType {
            returnType = type
            receiverType = type
        }
    }

    fun testBasicNullableFunWithUserReturnType(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.functionType {
            returnType = type
            isMarkedNullable = true
        }
    }

    fun testWithDefaultValues(): KaType {
        return session.typeCreator.functionType()
    }

    fun testReflectWithDefaultValues(): KaType {
        return session.typeCreator.functionType {
            isReflectType = true
        }
    }

    fun testSuspendWithDefaultValues(): KaType {
        return session.typeCreator.functionType {
            isSuspend = true
        }
    }

    fun testReflectAndSuspendWithDefaultValues(): KaType {
        return session.typeCreator.functionType {
            isReflectType = true
            isSuspend = true
        }
    }

    fun testFourIntValueParameters(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.functionType {
            valueParameter(Name.identifier("first"), type)
            valueParameter(Name.identifier("second")) {
                type
            }
            valueParameter(Name.identifier("third")) {
                type
            }
            valueParameter(Name.identifier("fourth"), type)
        }
    }

    fun testWithSingleContextParameter(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.functionType {
            contextParameter {
                type
            }
        }
    }

    fun testReflectWithSingleContextParameter(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.functionType {
            contextParameter {
                type
            }

            isReflectType = true
        }
    }

    fun testWithContextParameterReceiverAndValueParameter(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.functionType {
            contextParameter(type)
            receiverType = type
            valueParameter(Name.identifier("myParameter"), type)
        }
    }

    fun testWithPluginAnnotation(): KaType {
        val annotationClassId = PluginFunctionalNames.MY_INLINEABLE_ANNOTATION_CLASS_ID
        return session.typeCreator.functionType {
            annotation(annotationClassId)
        }
    }

    fun testReflectWithStringReceiver(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.functionType {
            receiverType = type
            isReflectType = true
        }
    }

    fun testWithAnnotation(): KaType {
        val annotationClassId = ClassId.fromString("MyAnno")
        return session.typeCreator.functionType {
            annotation { annotationClassId }
        }
    }

    fun testWithAnnotationReceiverAndContextParameter(): KaType {
        val annotationClassId = ClassId.fromString("MyAnno")
        val type = getTypeByCaret("type")
        return session.typeCreator.functionType {
            receiverType = type
            contextParameter { type }
            annotation(annotationClassId)
        }
    }
}