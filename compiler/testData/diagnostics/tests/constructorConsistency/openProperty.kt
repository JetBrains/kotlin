abstract class Base {
    abstract var x: Int

    abstract var y: Int

    constructor() {
        <!DEBUG_INFO_LEAKING_THIS!>x<!> = 42
        this.<!DEBUG_INFO_LEAKING_THIS!>y<!> = 24
        val temp = this.<!DEBUG_INFO_LEAKING_THIS!>x<!>
        this.<!DEBUG_INFO_LEAKING_THIS!>x<!> = <!DEBUG_INFO_LEAKING_THIS!>y<!>
        <!DEBUG_INFO_LEAKING_THIS!>y<!> = temp
    }
}
