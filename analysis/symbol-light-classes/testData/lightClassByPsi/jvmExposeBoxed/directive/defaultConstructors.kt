// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class Id(val value: String)

class Regular(val id: Id = Id("regular"))

enum class MyEnum(val id: Id = Id("default")) {
    EXPLICIT(Id("explicit")),
    DEFAULT,
}

class Delegating(val id: Id) {
    constructor() : this(Id("delegating"))
}

class ResultHolder(val result: Result<String> = Result.success("result"))

// LIGHT_ELEMENTS_NO_DECLARATION: Delegating.class[getId-eEFUqEU], Id.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], MyEnum.class[getEntries;getId-eEFUqEU;valueOf;values], Regular.class[getId-eEFUqEU], ResultHolder.class[getResult-d1pmJ48]
