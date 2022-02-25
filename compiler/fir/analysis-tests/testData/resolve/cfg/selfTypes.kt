// !DUMP_CFG

import kotlin.Self

@Self
class ReturnType {

    fun returnType(): Self = this

}

@Self
class WithTypeParameter<T : WithTypeParameter<T>> {

    fun withTypeParameter: Self = this

}

@Self
class WithoutTypeParameter<T : WithoutTypeParameter> {

    fun withoutTypeParameter: Self = this

}