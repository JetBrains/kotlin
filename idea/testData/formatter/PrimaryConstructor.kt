class A1  ()

class A2 private    ()
class A3 private @[ann] protected constructor()
class A4 private @ann1         protected        @[ann2] constructor() {

}
class A5 private @ann constructor

()

class A6     /* faddf */    private ()

class A7
private
@ann constructor
()

class A8     // eol comment
private
@ann constructor
()

class A9 // eol comment
private // eol comment
@ann constructor// eol comment
()

class A10
protected        @ann   constructor    ()

class A11
protected
@ann
constructor
()
