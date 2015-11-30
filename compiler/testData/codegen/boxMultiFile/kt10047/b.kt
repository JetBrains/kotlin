package test

open class Actor

abstract public class O2dScriptAction<T : Actor> {
    protected var owner: T? = null
        private set

    protected fun calc(): T? = null

}