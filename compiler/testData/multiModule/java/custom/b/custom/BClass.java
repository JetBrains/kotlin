package custom;

public class BClass extends AClass {
    public AClass returnA() {}
    public void paramA(AClass a) {}
    @AAnnotation(AEnum.AX)
    public void annoA() {}
    public BClass returnB() {}
    public void paramB(BClass b) {}
    @BAnnotation
    public void annoB() {}
}