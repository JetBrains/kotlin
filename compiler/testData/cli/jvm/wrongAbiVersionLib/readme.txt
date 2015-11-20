/*
To regenerate this test:
  1. Change JvmAbi.VERSION to an incompatible one, e.g. 0.30.0
  2. Run 'ant dist'
  3. Compile files with the newly built compiler from dist/

    cd compiler/testData/cli/jvm/wrongAbiVersionLib
    ../../../../../dist/kotlinc/bin/kotlinc -d bin src/*

  4. Change JvmAbi.VERSION back to its old value
  5. Run 'ant dist'
After these steps, the test should succeed
*/
