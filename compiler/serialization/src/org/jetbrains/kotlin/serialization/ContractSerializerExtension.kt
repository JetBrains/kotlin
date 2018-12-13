/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.contracts.description.ContractDescription
import org.jetbrains.kotlin.contracts.description.ExtensionEffectDeclaration
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf

interface ContractSerializerExtension {
    companion object : ProjectExtensionDescriptor<ContractSerializerExtension>(
        "org.jetbrains.kotlin.serialization.contractsExtension",
        ContractSerializerExtension::class.java
    )

    /**
     * @return true if serialization ends successfully and false otherwise
     */
    fun serializeExtensionEffect(
        builder: ProtoBuf.Effect.Builder,
        effectDeclaration: ExtensionEffectDeclaration,
        contractDescription: ContractDescription,
        project: Project,
        contractSerializerWorker: ContractSerializer.ContractSerializerWorker,
        descriptorSerializer: DescriptorSerializer
    ): Boolean
}