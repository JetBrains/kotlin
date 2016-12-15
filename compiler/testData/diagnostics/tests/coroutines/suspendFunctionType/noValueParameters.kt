typealias Test1 = suspend <!UNSUPPORTED!>(Int) -> Unit<!>
typealias Test2 = suspend <!UNSUPPORTED!>Int.(Int) -> Unit<!>
typealias Test3 = List<suspend <!UNSUPPORTED!>(Int) -> Unit<!>>
typealias Test4 = List<suspend <!UNSUPPORTED!>Int.(Int) -> Unit<!>>