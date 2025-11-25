// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@get:JvmExposeBoxed("getter")
@set:JvmExposeBoxed("setter")
var foo: StringWrapper
    get() = StringWrapper("str")
    set(value) {

    }

// LIGHT_ELEMENTS_NO_DECLARATION: GlobalVariableKt.class[getFoo;getter;setFoo-JELJCFg;setter], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
// DECLARATIONS_NO_LIGHT_ELEMENTS: GlobalVariableKt.class[foo]