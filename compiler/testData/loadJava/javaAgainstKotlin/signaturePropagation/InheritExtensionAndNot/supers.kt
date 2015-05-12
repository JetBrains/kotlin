package test

public interface Super1 {
    public fun String.foo()
    public fun Array<String>.bar()
}

public interface Super2 {
    public fun foo(p: String)
    public fun bar(vararg p: String)
}

