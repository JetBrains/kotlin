// SKIP_TXT

public class Foo1 () {}
public class Foo2 constructor() {}
public class Foo3 public constructor() {}
public class Foo4 private constructor() {}

public class Foo5 {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>constructor<!>() {}
}

public class Foo6 {
    public constructor() {}
}