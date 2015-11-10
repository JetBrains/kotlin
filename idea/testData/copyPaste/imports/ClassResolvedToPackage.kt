package a

class a {
    companion object {
    }
}

<selection>fun f(i: a) {
    // TODO References shortening doesn't work for package vs class conflicts under the new resolution rules.
    // After importing 'a.a', expression 'a.a' is unresolved (since 'a' becomes a class).
    // 'package' in expression syntax might be required to fix it properly.
    a
    a()
}</selection>