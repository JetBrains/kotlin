trait A
class AImpl : A

class B : <!UNSUPPORTED!>A by AImpl()<!> {
    constructor() {}
}
