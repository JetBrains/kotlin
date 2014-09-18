package foo.bar.goo

type foo = bar
type foo<T> = bar
type foo<T : foo> = bar
type foo<A, B> = bar
type foo<A, B : A> = bar

type foo = bar ;
type foo<T> = bar ;

type foo<T : foo> = bar ;
type foo<A, B> = bar ;
type foo<A, B : A> = bar ;
