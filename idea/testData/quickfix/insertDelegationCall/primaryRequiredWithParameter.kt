// "Insert 'this()' call" "true"
// ERROR: <html>None of the following functions can be called with the arguments supplied. <ul><li><init>(<font color=red><b>Int</b></font>) <i>defined in</i> A</li><li><init>(<font color=red><b>String</b></font>) <i>defined in</i> A</li></ul></html>

class A(val x: Int) {
    constructor(x: String)<caret>
}
