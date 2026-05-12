// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// JVM_EXPOSE_BOXED

// FILE: Test.kt
@JvmInline
value class Id(val value: String)

class Regular(val id: Id = Id("3"))

enum class MyEnum(val id: Id = Id("2")) {
    ONE(Id("1")),
    TWO()
}

class Delegating(val id: Id) {
    constructor() : this(Id("5"))
}

class ResultHolder(val result: Result<String> = Result.success("6"))

// FILE: J.java
public class J {
    public String one() {
        return MyEnum.ONE.getId().getValue();
    }
    public String two() {
        return MyEnum.TWO.getId().getValue();
    }
    public String three() {
        return new Regular().getId().getValue();
    }
    public String four() {
        return new Regular(new Id("4")).getId().getValue();
    }
    public String five() {
        return new Delegating().getId().getValue();
    }
    public String six() {
        return new ResultHolder().getResult().toString();
    }
}

// FILE: box.kt
fun box(): String {
    var result = J().one()
    if (result != "1") return "FAIL 1: $result"
    result = J().two()
    if (result != "2") return "FAIL 2: $result"
    result = J().three()
    if (result != "3") return "FAIL 3: $result"
    result = J().four()
    if (result != "4") return "FAIL 4: $result"
    result = J().five()
    if (result != "5") return "FAIL 5: $result"
    result = J().six()
    if (result != "Success(6)") return "FAIL 6: $result"
    return "OK"
}
