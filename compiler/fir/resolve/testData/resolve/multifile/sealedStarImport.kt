package other

import test.Test.*

abstract class Factory {
    abstract fun createTest(): Test

    abstract fun createObj(): O

    abstract fun createExtra(): Extra
}