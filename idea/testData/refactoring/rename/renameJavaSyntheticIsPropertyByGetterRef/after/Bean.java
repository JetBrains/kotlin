public class Bean {
    private boolean prop;
    public boolean isProp2() { return prop; }
    public void setProp2(boolean prop) { this.prop = prop; }

    void test() {
        isProp2();
        setProp2(true);
    }
}