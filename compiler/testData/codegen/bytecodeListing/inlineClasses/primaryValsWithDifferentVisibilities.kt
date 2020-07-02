// !LANGUAGE: +InlineClasses

interface IValue {
    val value: Int
}

inline class TestOverriding(override val value: Int) : IValue

inline class TestPublic(val value: Int)

inline class TestInternal(internal val value: Int)

inline class TestPrivate(private val value: Int)