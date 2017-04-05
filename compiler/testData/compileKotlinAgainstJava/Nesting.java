package test;

public class Nesting {

    public test.Nesting.Second.Third.FourthImpl getImpl() { return null; }

    public static final class Second {

        public static final class Third {

            public interface Fourth {
                public boolean isImplemented();
            }

            public static final class FourthImpl implements Fourth {

                @Override
                public boolean isImplemented() { return true; }

            }

        }

    }

}
