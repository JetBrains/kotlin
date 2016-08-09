/**
  ******************************************************************************
  * @file    stm32f4xx_wwdg.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the Window watchdog (WWDG) peripheral:           
  *           - Prescaler, Refresh window and Counter configuration
  *           - WWDG activation
  *           - Interrupts and flags management
  *             
  *  @verbatim
  *    
  *          ===================================================================
  *                                     WWDG features
  *          ===================================================================
  *                                        
  *          Once enabled the WWDG generates a system reset on expiry of a programmed
  *          time period, unless the program refreshes the counter (downcounter) 
  *          before to reach 0x3F value (i.e. a reset is generated when the counter
  *          value rolls over from 0x40 to 0x3F). 
  *          An MCU reset is also generated if the counter value is refreshed
  *          before the counter has reached the refresh window value. This 
  *          implies that the counter must be refreshed in a limited window.
  *            
  *          Once enabled the WWDG cannot be disabled except by a system reset.                          
  *          
  *          WWDGRST flag in RCC_CSR register can be used to inform when a WWDG
  *          reset occurs.
  *            
  *          The WWDG counter input clock is derived from the APB clock divided 
  *          by a programmable prescaler.
  *              
  *          WWDG counter clock = PCLK1 / Prescaler
  *          WWDG timeout = (WWDG counter clock) * (counter value)
  *                      
  *          Min-max timeout value @42 MHz(PCLK1): ~97.5 us / ~49.9 ms
  *                            
  *          ===================================================================
  *                                 How to use this driver
  *          =================================================================== 
  *          1. Enable WWDG clock using RCC_APB1PeriphClockCmd(RCC_APB1Periph_WWDG, ENABLE) function
  *            
  *          2. Configure the WWDG prescaler using WWDG_SetPrescaler() function
  *                           
  *          3. Configure the WWDG refresh window using WWDG_SetWindowValue() function
  *            
  *          4. Set the WWDG counter value and start it using WWDG_Enable() function.
  *             When the WWDG is enabled the counter value should be configured to 
  *             a value greater than 0x40 to prevent generating an immediate reset.     
  *            
  *          5. Optionally you can enable the Early wakeup interrupt which is 
  *             generated when the counter reach 0x40.
  *             Once enabled this interrupt cannot be disabled except by a system reset.
  *                 
  *          6. Then the application program must refresh the WWDG counter at regular
  *             intervals during normal operation to prevent an MCU reset, using
  *             WWDG_SetCounter() function. This operation must occur only when
  *             the counter value is lower than the refresh window value, 
  *             programmed using WWDG_SetWindowValue().         
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
#include "stm32f4xx_wwdg.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup WWDG 
  * @brief WWDG driver modules
  * @{
  */

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/

/* ----------- WWDG registers bit address in the alias region ----------- */
#define WWDG_OFFSET       (WWDG_BASE - PERIPH_BASE)
/* Alias word address of EWI bit */
#define CFR_OFFSET        (WWDG_OFFSET + 0x04)
#define EWI_BitNumber     0x09
#define CFR_EWI_BB        (PERIPH_BB_BASE + (CFR_OFFSET * 32) + (EWI_BitNumber * 4))

/* --------------------- WWDG registers bit mask ------------------------ */
/* CFR register bit mask */
#define CFR_WDGTB_MASK    ((uint32_t)0xFFFFFE7F)
#define CFR_W_MASK        ((uint32_t)0xFFFFFF80)
#define BIT_MASK          ((uint8_t)0x7F)

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup WWDG_Private_Functions
  * @{
  */

/** @defgroup WWDG_Group1 Prescaler, Refresh window and Counter configuration functions
 *  @brief   Prescaler, Refresh window and Counter configuration functions 
 *
@verbatim   
 ===============================================================================
          Prescaler, Refresh window and Counter configuration functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Deinitializes the WWDG peripheral registers to their default reset values.
  * @param  None
  * @retval None
  */
void WWDG_DeInit(void)
{
  RCC_APB1PeriphResetCmd(RCC_APB1Periph_WWDG, ENABLE);
  RCC_APB1PeriphResetCmd(RCC_APB1Periph_WWDG, DISABLE);
}

/**
  * @brief  Sets the WWDG Prescaler.
  * @param  WWDG_Prescaler: specifies the WWDG Prescaler.
  *   This parameter can be one of the following values:
  *     @arg WWDG_Prescaler_1: WWDG counter clock = (PCLK1/4096)/1
  *     @arg WWDG_Prescaler_2: WWDG counter clock = (PCLK1/4096)/2
  *     @arg WWDG_Prescaler_4: WWDG counter clock = (PCLK1/4096)/4
  *     @arg WWDG_Prescaler_8: WWDG counter clock = (PCLK1/4096)/8
  * @retval None
  */
void WWDG_SetPrescaler(uint32_t WWDG_Prescaler)
{
  uint32_t tmpreg = 0;
  /* Check the parameters */
  assert_param(IS_WWDG_PRESCALER(WWDG_Prescaler));
  /* Clear WDGTB[1:0] bits */
  tmpreg = WWDG->CFR & CFR_WDGTB_MASK;
  /* Set WDGTB[1:0] bits according to WWDG_Prescaler value */
  tmpreg |= WWDG_Prescaler;
  /* Store the new value */
  WWDG->CFR = tmpreg;
}

/**
  * @brief  Sets the WWDG window value.
  * @param  WindowValue: specifies the window value to be compared to the downcounter.
  *   This parameter value must be lower than 0x80.
  * @retval None
  */
void WWDG_SetWindowValue(uint8_t WindowValue)
{
  __IO uint32_t tmpreg = 0;

  /* Check the parameters */
  assert_param(IS_WWDG_WINDOW_VALUE(WindowValue));
  /* Clear W[6:0] bits */

  tmpreg = WWDG->CFR & CFR_W_MASK;

  /* Set W[6:0] bits according to WindowValue value */
  tmpreg |= WindowValue & (uint32_t) BIT_MASK;

  /* Store the new value */
  WWDG->CFR = tmpreg;
}

/**
  * @brief  Enables the WWDG Early Wakeup interrupt(EWI).
  * @note   Once enabled this interrupt cannot be disabled except by a system reset.
  * @param  None
  * @retval None
  */
void WWDG_EnableIT(void)
{
  *(__IO uint32_t *) CFR_EWI_BB = (uint32_t)ENABLE;
}

/**
  * @brief  Sets the WWDG counter value.
  * @param  Counter: specifies the watchdog counter value.
  *   This parameter must be a number between 0x40 and 0x7F (to prevent generating
  *   an immediate reset) 
  * @retval None
  */
void WWDG_SetCounter(uint8_t Counter)
{
  /* Check the parameters */
  assert_param(IS_WWDG_COUNTER(Counter));
  /* Write to T[6:0] bits to configure the counter value, no need to do
     a read-modify-write; writing a 0 to WDGA bit does nothing */
  WWDG->CR = Counter & BIT_MASK;
}
/**
  * @}
  */

/** @defgroup WWDG_Group2 WWDG activation functions
 *  @brief   WWDG activation functions 
 *
@verbatim   
 ===============================================================================
                       WWDG activation function
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Enables WWDG and load the counter value.                  
  * @param  Counter: specifies the watchdog counter value.
  *   This parameter must be a number between 0x40 and 0x7F (to prevent generating
  *   an immediate reset)
  * @retval None
  */
void WWDG_Enable(uint8_t Counter)
{
  /* Check the parameters */
  assert_param(IS_WWDG_COUNTER(Counter));
  WWDG->CR = WWDG_CR_WDGA | Counter;
}
/**
  * @}
  */

/** @defgroup WWDG_Group3 Interrupts and flags management functions
 *  @brief   Interrupts and flags management functions 
 *
@verbatim   
 ===============================================================================
                 Interrupts and flags management functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Checks whether the Early Wakeup interrupt flag is set or not.
  * @param  None
  * @retval The new state of the Early Wakeup interrupt flag (SET or RESET)
  */
FlagStatus WWDG_GetFlagStatus(void)
{
  FlagStatus bitstatus = RESET;
    
  if ((WWDG->SR) != (uint32_t)RESET)
  {
    bitstatus = SET;
  }
  else
  {
    bitstatus = RESET;
  }
  return bitstatus;
}

/**
  * @brief  Clears Early Wakeup interrupt flag.
  * @param  None
  * @retval None
  */
void WWDG_ClearFlag(void)
{
  WWDG->SR = (uint32_t)RESET;
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
