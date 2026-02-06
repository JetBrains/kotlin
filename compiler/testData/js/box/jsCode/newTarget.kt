fun box(): String {
    js("""
        function RememberTarget() {
            this.target = new.target;
        }
    """)
    val instance = js("new RememberTarget()")
    assertEquals("function", js("typeof instance.target"))
    assertEquals("RememberTarget", js("instance.target.name"))
    return "OK"
}
