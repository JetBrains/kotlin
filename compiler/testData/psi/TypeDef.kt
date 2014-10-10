package foo.bar.goo

typealias foo = bar
typealias foo<T> = bar
typealias foo<T : foo> = bar
typealias foo<A, B> = bar
typealias foo<A, B : A> = bar

typealias foo = bar ;
typealias foo<T> = bar ;

typealias foo<T : foo> = bar ;
typealias foo<A, B> = bar ;
typealias foo<A, B : A> = bar ;
