interface I {
    enum class E {
        V { override fun go() { } };
        abstract fun go()
    }
}

// 0 public final static enum INNERCLASS I\$E I E
// 3 public static abstract enum INNERCLASS I\$E I E