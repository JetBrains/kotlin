// "Make 'My' final" "true"

open class My {

    init {
        <caret>init()
    }

    open fun init() {}
}