// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class ICInt(val i: Int) {
    fun toInt(): Int = i
}

@get:JvmExposeBoxed
val ICInt.extPropValAnnotated: Int
    get() = this.toInt()

@set:JvmExposeBoxed
var ICInt.extPropVarAnnotated: Int
    get() = this.toInt()
    set(value) {}

val ICInt.extPropValAnnotatedGetItself: Int
    @JvmExposeBoxed get() = this.toInt()

var ICInt.extPropVarAnnotatedSetItself: Int
    get() = this.toInt()
    @JvmExposeBoxed set(value) {}

context(ic: ICInt)
val ctxPropValAnnotatedGetItself: Int
    @JvmExposeBoxed get() = ic.toInt()

context(ic: ICInt)
var ctxPropVarAnnotatedSetItself: Int
    get() = ic.toInt()
    @JvmExposeBoxed set(value) {}

// DECLARATIONS_NO_LIGHT_ELEMENTS: ICInt.class[toInt]
// LIGHT_ELEMENTS_NO_DECLARATION: GlobalVariableReceiverAccessorAnnotatedKt.class[getCtxPropValAnnotatedGetItself-LakxRoo;getCtxPropVarAnnotatedSetItself-LakxRoo;getExtPropValAnnotated-LakxRoo;getExtPropValAnnotatedGetItself-LakxRoo;getExtPropVarAnnotated-LakxRoo;getExtPropVarAnnotatedSetItself-LakxRoo;setCtxPropVarAnnotatedSetItself-bcwIPng;setExtPropVarAnnotated-bcwIPng;setExtPropVarAnnotatedSetItself-bcwIPng], ICInt.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toInt-impl;toString-impl]
