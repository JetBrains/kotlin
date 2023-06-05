// RENDER_FIR_DECLARATION_ATTRIBUTES
// PLATFORM_DEPENDANT_METADATA
// NO_CHECK_SOURCE_VS_BINARY
// SKIP_IN_RUNTIME_TEST

package test

internal class Internal @PublishedApi internal constructor(val foo: String) {
    @PublishedApi internal constructor() : this("")
    @PublishedApi internal fun method() {}
    @PublishedApi internal val prop: Int = 1
    @PublishedApi internal val prop2: Int get() = 1
    @PublishedApi internal var prop3: Int = 1
        set(value) {}

    @PublishedApi internal class Nested
}

class Public @PublishedApi internal constructor(val foo: String) {
    @PublishedApi internal constructor() : this("")
    @PublishedApi internal fun method() {}
    @PublishedApi internal val prop: Int = 1
    @PublishedApi internal val prop2: Int get() = 1
    @PublishedApi internal var prop3: Int = 1
        set(value) {}

    @PublishedApi internal class Nested
}

@PublishedApi
internal class Published @PublishedApi internal constructor(val foo: String) {
    @PublishedApi internal constructor() : this("")
    @PublishedApi internal fun method() {}
    @PublishedApi internal val prop: Int = 1
    @PublishedApi internal val prop2: Int get() = 1
    @PublishedApi internal var prop3: Int = 1
        set(value) {}

    @PublishedApi internal class Nested
}

@PublishedApi internal fun method() {}
@PublishedApi internal val prop: Int = 1
@PublishedApi internal val prop2: Int get() = 1
@PublishedApi internal var prop3: Int = 1
    set(value) {}