// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

typealias L<T> = List<T>

class Outer {
    private class Private
    protected class Protected
    internal class Internal

    typealias TestPrivate1 = Private
    protected typealias TestPrivate2 = Private
    internal typealias TestPrivate3 = Private
    private typealias TestPrivate4 = Private
    typealias TestPrivate5 = L<Private>
    typealias TestPrivate6 = L<TestPrivate1>

    typealias TestProtected1 = Protected
    protected typealias TestProtected2 = Protected
    internal typealias TestProtected3 = Protected
    private typealias TestProtected4 = Protected
    typealias TestProtected5 = L<Protected>
    typealias TestProtected6 = L<TestProtected1>

    typealias TestInternal1 = Internal
    protected typealias TestInternal2 = Internal
    internal typealias TestInternal3 = Internal
    private typealias TestInternal4 = Internal
    typealias TestInternal5 = L<Internal>
    typealias TestInternal6 = L<TestInternal1>
}

private class Private
internal class Internal

typealias TestPrivate1 = Private
internal typealias TestPrivate2 = Private
private typealias TestPrivate3 = Private
typealias TestPrivate4 = L<Private>
typealias TestPrivate5 = L<TestPrivate1>

typealias TestInternal1 = Internal
internal typealias TestInternal2 = Internal
private typealias TestInternal3 = Internal
typealias TestInternal4 = L<Internal>
typealias TestInternal5 = L<TestInternal1>
