fun a() <fold text='{...}'>{
    <Foo foo="bar" bam="baz" hndlr={ a -> println(a) } />

    <Foo foo="bar" bam="baz" hndlr={ a -> println(a) }><Bar /></Foo>

    <Foo<fold text=' foo="bar" bam="baz" hndlr={ a -> println(a) } '>
        foo="bar"
        bam="baz"
        hndlr={ a -> println(a) }
    </fold>/>

    <Foo><fold text='...'>
        <Bar />
    </fold></Foo>

    <Foo foo="bar"><fold text='...'>
        <Bar />
    </fold></Foo>

    <Foo<fold text=' foo="bar" bam="baz" hndlr={ a -> println(a) }>...'>
        foo="bar"
        bam="baz"
        hndlr={ a -> println(a) }
    >
        <Bar />
    </fold></Foo>

    <LinearLayout></LinearLayout>

    <Foo<fold text=' foo="bar" bam="baz" hndlr=...>...'>
        foo="bar"
        bam="baz"
        hndlr={ a -> printawjdoiajdoijaodijaoidjaoiwjdoaiwjdoiawjdoiajwdoiajwodijawodijawoidjaoiwjdoaiwdln(a) }
    >
        <Bar />
    </fold></Foo>
}</fold>


fun a() <fold text='{...}'>{
    // invalid ktx element case
    <fold text='</>'><Foo>
</fold>}</fold>
