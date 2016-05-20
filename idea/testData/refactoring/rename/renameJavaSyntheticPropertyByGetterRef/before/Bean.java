public class Bean {
    private String prop = "value";
    public String getProp() { return prop; }
    public void setProp(String prop) { this.prop = prop; }

    void test() {
        getProp();
        setProp("");
    }
}