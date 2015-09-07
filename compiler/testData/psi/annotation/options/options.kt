annotation class base

annotation() class empty

annotation(repeatable = true) class ann

annotation(Retention.BINARY, false) class ann2

annotation(retention = Retention.RUNTIME) class ann3

@Target(Target.FUNCTION, Target.CLASSIFIER, Target.EXPRESSION)
annotation(Retention.SOURCE) class ann4