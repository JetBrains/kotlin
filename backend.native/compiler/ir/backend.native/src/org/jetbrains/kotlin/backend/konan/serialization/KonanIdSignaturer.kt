package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.ir.util.KotlinMangler

class KonanIdSignaturer(mangler: KotlinMangler.DescriptorMangler) : IdSignatureDescriptor(mangler) {

}