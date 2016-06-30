/**
  ******************************************************************************
  * @file    stm32f4xx_rng.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
    * @brief This file provides firmware functions to manage the following 
  *          functionalities of the Random Number Generator (RNG) peripheral:           
  *           - Initialization and Configuration 
  *           - Get 32 bit Random number      
  *           - Interrupts and flags management       
  *         
  *  @verbatim
  *                               
  *          ===================================================================      
  *                                   How to use this driver
  *          ===================================================================          
  *          1. Enable The RNG controller clock using 
  *            RCC_AHB2PeriphClockCmd(RCC_AHB2Periph_RNG, ENABLE) function.
  *              
  *          2. Activate the RNG peripheral using RNG_Cmd() function.
  *          
  *          3. Wait until the 32 bit Random number Generator contains a valid 
  *            random data (using polling/interrupt mode). For more details, 
  *            refer to "Interrupts and flags management functions" module 
  *            description.
  *           
  *          4. Get the 32 bit Random number using RNG_GetRandomNumber() function
  *          
  *          5. To get another 32 bit Random number, go to step 3.       
  *
  *         
  *              
  *  @endverbatim
  *         
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************  
  */

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx_conf.h"
#include "stm32f4xx_rng.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup RNG 
  * @brief RNG driver modules
  * @{
  */ 

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/
/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup RNG_Private_Functions
  * @{
  */ 

/** @defgroup RNG_Group1 Initialization and Configuration functions
 *  @brief    Initialization and Configuration functions 
 *
@verbatim    
 ===============================================================================
                      Initialization and Configuration functions
 ===============================================================================  
  This section provides functions allowing to 
   - Initialize the RNG peripheral
   - Enable or disable the RNG peripheral
   
@endverbatim
  * @{
  */

/**
  * @brief  Deinitializes the RNG peripheral registers to their default reset values.
  * @param  None
  * @retval None
  */
void RNG_DeInit(void)
{
  /* Enable RNG reset state */
  RCC_AHB2PeriphResetCmd(RCC_AHB2Periph_RNG, ENABLE);

  /* Release RNG from reset state */
  RCC_AHB2PeriphResetCmd(RCC_AHB2Periph_RNG, DISABLE);
}

/**
  * @brief  Enables or disables the RNG peripheral.
  * @param  NewState: new state of the RNG peripheral.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void RNG_Cmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Enable the RNG */
    RNG->CR |= RNG_CR_RNGEN;
  }
  else
  {
    /* Disable the RNG */
    RNG->CR &= ~RNG_CR_RNGEN;
  }
}
/**
  * @}
  */

/** @defgroup RNG_Group2 Get 32 bit Random number function
 *  @brief    Get 32 bit Random number function 
 *

@verbatim    
 ===============================================================================
                      Get 32 bit Random number function
 ===============================================================================  
  This section provides a function allowing to get the 32 bit Random number  
  
  @note  Before to call this function you have to wait till DRDY flag is set,
         using RNG_GetFlagStatus(RNG_FLAG_DRDY) function. 
   
@endverbatim
  * @{
  */


/**
  * @brief  Returns a 32-bit random number.
  *   
  * @note   Before to call this function you have to wait till DRDY (data ready)
  *         flag is set, using RNG_GetFlagStatus(RNG_FLAG_DRDY) function.
  * @note   Each time the the Random number data is read (using RNG_GetRandomNumber()
  *         function), the RNG_FLAG_DRDY flag is automatically cleared.
  * @note   In the case of a seed error, the generation of random numbers is 
  *         interrupted for as long as the SECS bit is '1'. If a number is 
  *         available in the RNG_DR register, it must not be used because it may 
  *         not have enough entropy. In this case, it is recommended to clear the 
  *         SEIS bit(using RNG_ClearFlag(RNG_FLAG_SECS) function), then disable 
  *         and enable the RNG peripheral (using RNG_Cmd() function) to 
  *         reinitialize and restart the RNG.
  * @note   In the case of a clock error, the RNG is no more able to generate 
  *         random numbers because the PLL48CLK clock is not correct. User have 
  *         to check that the clock controller is correctly configured to provide
  *         the RNG clock and clear the CEIS bit (using RNG_ClearFlag(RNG_FLAG_CECS) 
  *         function) . The clock error has no impact on the previously generated 
  *         random numbers, and the RNG_DR register contents can be used.
  *         
  * @param  None
  * @retval 32-bit random number.
  */
uint32_t RNG_GetRandomNumber(void)
{
  /* Return the 32 bit random number from the DR register */
  return RNG->DR;
}


/**
  * @}
  */

/** @defgroup RNG_Group3 Interrupts and flags management functions
 *  @brief   Interrupts and flags management functions
 *
@verbatim   
 ===============================================================================
                   Interrupts and flags management functions
 ===============================================================================  

  This section provides functions allowing to configure the RNG Interrupts and 
  to get the status and clear flags and Interrupts pending bits.
  
  The RNG provides 3 Interrupts sources and 3 Flags:
  
  Flags :
  ---------- 
     1. RNG_FLAG_DRDY :  In the case of the RNG_DR register contains valid 
                         random data. it is cleared by reading the valid data 
                         (using RNG_GetRandomNumber() function).

     2. RNG_FLAG_CECS : In the case of a seed error detection. 
      
     3. RNG_FLAG_SECS : In the case of a clock error detection.
              

  Interrupts :
  ------------
   if enabled, an RNG interrupt is pending :
    
   1.  In the case of the RNG_DR register contains valid random data. 
       This interrupt source is cleared once the RNG_DR register has been read 
       (using RNG_GetRandomNumber() function) until a new valid value is 
       computed. 
   
   or 
   2. In the case of a seed error : One of the following faulty sequences has 
      been detected:
      - More than 64 consecutive bits at the same value (0 or 1)
      - More than 32 consecutive alternance of 0 and 1 (0101010101...01)
      This interrupt source is cleared using RNG_ClearITPendingBit(RNG_IT_SEI)
      function.
   
   or
   3. In the case of a clock error : the PLL48CLK (RNG peripheral clock source) 
      was not correctly detected (fPLL48CLK< fHCLK/16).
      This interrupt source is cleared using RNG_ClearITPendingBit(RNG_IT_CEI)
      function.
      @note In this case, User have to check that the clock controller is 
            correctly configured to provide the RNG clock. 

  Managing the RNG controller events :
  ------------------------------------ 
  The user should identify which mode will be used in his application to manage 
  the RNG controller events: Polling mode or Interrupt mode.
  
  1.  In the Polling Mode it is advised to use the following functions:
      - RNG_GetFlagStatus() : to check if flags events occur. 
      - RNG_ClearFlag()     : to clear the flags events.
  
  @note RNG_FLAG_DRDY can not be cleared by RNG_ClearFlag(). it is cleared only 
        by reading the Random number data.      
  
  2.  In the Interrupt Mode it is advised to use the following functions:
      - RNG_ITConfig()       : to enable or disable the interrupt source.
      - RNG_GetITStatus()    : to check if Interrupt occurs.
      - RNG_ClearITPendingBit() : to clear the Interrupt pending Bit 
                                (corresponding Flag). 
  

@endverbatim
  * @{
  */ 

/**
  * @brief  Enables or disables the RNG interrupt.
  * @note   The RNG provides 3 interrupt sources,
  *           - Computed data is ready event (DRDY), and           
  *           - Seed error Interrupt (SEI) and 
  *           - Clock error Interrupt (CEI), 
  *         all these interrupts sources are enabled by setting the IE bit in 
  *         CR register. However, each interrupt have its specific status bit
  *         (see RNG_GetITStatus() function) and clear bit except the DRDY event
  *         (see RNG_ClearITPendingBit() function).
  * @param  NewState: new state of the RNG interrupt.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void RNG_ITConfig(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Enable the RNG interrupt */
    RNG->CR |= RNG_CR_IE;
  }
  else
  {
    /* Disable the RNG interrupt */
    RNG->CR &= ~RNG_CR_IE;
  }
}

/**
  * @brief  Checks whether the specified RNG flag is set or not.
  * @param  RNG_FLAG: specifies the RNG flag to check.
  *          This parameter can be one of the following values:
  *            @arg RNG_FLAG_DRDY: Data Ready flag.
  *            @arg RNG_FLAG_CECS: Clock Error Current flag.
  *            @arg RNG_FLAG_SECS: Seed Error Current flag.
  * @retval The new state of RNG_FLAG (SET or RESET).
  */
FlagStatus RNG_GetFlagStatus(uint8_t RNG_FLAG)
{
  FlagStatus bitstatus = RESET;
  /* Check the parameters */
  assert_param(IS_RNG_GET_FLAG(RNG_FLAG));

  /* Check the status of the specified RNG flag */
  if ((RNG->SR & RNG_FLAG) != (uint8_t)RESET)
  {
    /* RNG_FLAG is set */
    bitstatus = SET;
  }
  else
  {
    /* RNG_FLAG is reset */
    bitstatus = RESET;
  }
  /* Return the RNG_FLAG status */
  return  bitstatus;
}


/**
  * @brief  Clears the RNG flags.
  * @param  RNG_FLAG: specifies the flag to clear. 
  *          This parameter can be any combination of the following values:
  *            @arg RNG_FLAG_CECS: Clock Error Current flag.
  *            @arg RNG_FLAG_SECS: Seed Error Current flag.
  * @note   RNG_FLAG_DRDY can not be cleared by RNG_ClearFlag() function. 
  *         This flag is cleared only by reading the Random number data (using 
  *         RNG_GetRandomNumber() function).                           
  * @retval None
  */
void RNG_ClearFlag(uint8_t RNG_FLAG)
{
  /* Check the parameters */
  assert_param(IS_RNG_CLEAR_FLAG(RNG_FLAG));
  /* Clear the selected RNG flags */
  RNG->SR = ~(uint32_t)(((uint32_t)RNG_FLAG) << 4);
}

/**
  * @brief  Checks whether the specified RNG interrupt has occurred or not.
  * @param  RNG_IT: specifies the RNG interrupt source to check.
  *          This parameter can be one of the following values:
  *            @arg RNG_IT_CEI: Clock Error Interrupt.
  *            @arg RNG_IT_SEI: Seed Error Interrupt.                   
  * @retval The new state of RNG_IT (SET or RESET).
  */
ITStatus RNG_GetITStatus(uint8_t RNG_IT)
{
  ITStatus bitstatus = RESET;
  /* Check the parameters */
  assert_param(IS_RNG_GET_IT(RNG_IT));

  /* Check the status of the specified RNG interrupt */
  if ((RNG->SR & RNG_IT) != (uint8_t)RESET)
  {
    /* RNG_IT is set */
    bitstatus = SET;
  }
  else
  {
    /* RNG_IT is reset */
    bitstatus = RESET;
  }
  /* Return the RNG_IT status */
  return bitstatus;
}


/**
  * @brief  Clears the RNG interrupt pending bit(s).
  * @param  RNG_IT: specifies the RNG interrupt pending bit(s) to clear.
  *          This parameter can be any combination of the following values:
  *            @arg RNG_IT_CEI: Clock Error Interrupt.
  *            @arg RNG_IT_SEI: Seed Error Interrupt.
  * @retval None
  */
void RNG_ClearITPendingBit(uint8_t RNG_IT)
{
  /* Check the parameters */
  assert_param(IS_RNG_IT(RNG_IT));

  /* Clear the selected RNG interrupt pending bit */
  RNG->SR = (uint8_t)~RNG_IT;
}
/**
  * @}
  */ 
  
/**
  * @}
  */ 

/**
  * @}
  */ 


/**
  * @}
  */ 


/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
