// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER, -PLATFORM_CLASS_MAPPED_TO_KOTLIN
// WITH_STDLIB

package kotlin.jvm

annotation class JvmInline

<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>inline<!> class IC0(val a: Any): Cloneable

@JvmInline
<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>value<!> class VC0(val a: Any): Cloneable

<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>inline<!> class IC1(val a: Any): java.lang.Cloneable

@JvmInline
<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>value<!> class VC1(val a: Any): java.lang.Cloneable

interface MyCloneable1: Cloneable

<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>inline<!> class IC2(val a: Any): MyCloneable1

@JvmInline
<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>value<!> class VC2(val a: Any): MyCloneable1

interface MyCloneable2: java.lang.Cloneable

<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>inline<!> class IC3(val a: Any): MyCloneable2

@JvmInline
<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>value<!> class VC3(val a: Any): MyCloneable2