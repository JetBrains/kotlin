package test;

public class ReturnInnerInner {

    public class InnerFirst {

        public class InnerSecond {
            
        }

    }

    public static InnerFirst.InnerSecond getInnerInner() { return null; }

}
