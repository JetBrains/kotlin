// "Make Private protected" "true"

class Receiver<T>

abstract class My {
    private class Private

    abstract protected fun <caret>Receiver<Private>.foo()
}