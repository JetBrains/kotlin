enum class Event {
    ON_CREATE, ON_START, ON_STOP, ON_DESTROY;
    companion object {
        @JvmStatic
        fun upTo(state: State): Event? {
            return when(state) {
                State.ENQUEUED -> ON_CREATE
                State.RUNNING -> ON_START
                State.BLOCKED -> ON_STOP
                else -> null
            }
        }
    }
}

enum class State {
    ENQUEUED, RUNNING, SUCCEEDED, FAILED, BLOCKED, CANCELLED;
    val isFinished: Boolean
        get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
    fun isAtLeast(state: State): Boolean {
        return compareTo(state) >= 0
    }
    companion object {
        fun done(state: State) = state.isFinished
    }
}
