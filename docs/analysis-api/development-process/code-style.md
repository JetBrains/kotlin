# Analysis API Code Style

This is a code style for Analysis API modules which are located in [analysis directory](../../../analysis)

* Please, follow [official Kotlin code style](https://kotlinlang.org/docs/coding-conventions.html).
* IntelliJ IDEA is set up for these coding guidelines and the default formatter will help you a lot.
* Consider using introducing explicit lambda parameter instead of implicit `it` parameter when:
    * The lambda is multi-line, so includes multiple statements
    * The `it` parameter is used multiple times inside lambda
* Do not overuse `let`, `also`, `apply` and similar functions. 
  It seems cool to write a complex multiline function as a single expression but please, do not sacrifice code readability because of that.
