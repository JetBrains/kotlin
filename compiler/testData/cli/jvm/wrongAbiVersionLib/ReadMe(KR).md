/*
이 테스트를 다시 생성하려면 :

   1. JvmAbi.VERSION을 호환되지 않는 버전으로 변경하십시오 (예: 0.30.0)

   2. 'ant dist'를 실행합니다.
   
   3. dist/에서 새롭게 설정된 컴파일러로 파일들을 컴파일합니다.
   
     cd compiler/testData/cli/jvm/wrongAbiVersionLib
     ../../../../../dist/kotlinc/bin/kotlinc -d bin src/*
     
   4. JvmAbi.VERSION을 이전 값으로 변경하십시오.
   
   5. 'ant dist'를 실행합니다.
   
이 단계가 끝나면 테스트가 성공해야합니다.

*/
