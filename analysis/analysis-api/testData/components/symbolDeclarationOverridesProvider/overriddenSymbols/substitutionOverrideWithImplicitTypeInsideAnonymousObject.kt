package second

class MyClass {
    val prop = object : LazySchemeProcessor<Int, Int>() {
        override fun is<caret>SchemeFile(name: CharSequence) = name != "str"
    }
}

abstract class LazySchemeProcessor<SCHEME : Number, MUTABLE_SCHEME : SCHEME> {
    open fun isSchemeFile(name: CharSequence) = true
}