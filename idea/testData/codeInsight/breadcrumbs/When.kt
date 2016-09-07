fun foo() {
    when {
        else -> {
            when (x) {
                x.isSomethingLong() -> {
                    when (y) {
                        y.isSomething() -> {
                            when (z) {
                                is VeryVeryVeryVeryLongName -> {
                                    when (xxx) {
                                        SomeEnum.SomeLongValue -> {
                                            when (yyy) {
                                                !in Int.MIN_VALUE..Int.MAX_VALUE -> {
                                                    when (zzz) {
                                                        is A, is B -> {
                                                            when (x1) {
                                                                A.VeryVeryVeryLongName, A.ShortName -> {
                                                                    when (y1) {
                                                                        is VeryVeryVeryVeryLongClassName, is ShortName -> {
                                                                            <caret>
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}