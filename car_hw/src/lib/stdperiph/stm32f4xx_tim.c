/**
  ******************************************************************************
  * @file    stm32f4xx_tim.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the TIM peripheral:
  *            - TimeBase management
  *            - Output Compare management
  *            - Input Capture management
  *            - Advanced-control timers (TIM1 and TIM8) specific features  
  *            - Interrupts, DMA and flags management
  *            - Clocks management
  *            - Synchronization management
  *            - Specific interface management
  *            - Specific remapping management      
  *              
  *  @verbatim
  *  
  *          ===================================================================
  *                                 How to use this driver
  *          ===================================================================
  *          This driver provides functions to configure and program the TIM 
  *          of all STM32F4xx devices.
  *          These functions are split in 9 groups: 
  *   
  *          1. TIM TimeBase management: this group includes all needed functions 
  *             to configure the TM Timebase unit:
  *                   - Set/Get Prescaler
  *                   - Set/Get Autoreload  
  *                   - Counter modes configuration
  *                   - Set Clock division  
  *                   - Select the One Pulse mode
  *                   - Update Request Configuration
  *                   - Update Disable Configuration
  *                   - Auto-Preload Configuration 
  *                   - Enable/Disable the counter     
  *                 
  *          2. TIM Output Compare management: this group includes all needed 
  *             functions to configure the Capture/Compare unit used in Output 
  *             compare mode: 
  *                   - Configure each channel, independently, in Output Compare mode
  *                   - Select the output compare modes
  *                   - Select the Polarities of each channel
  *                   - Set/Get the Capture/Compare register values
  *                   - Select the Output Compare Fast mode 
  *                   - Select the Output Compare Forced mode  
  *                   - Output Compare-Preload Configuration 
  *                   - Clear Output Compare Reference
  *                   - Select the OCREF Clear signal
  *                   - Enable/Disable the Capture/Compare Channels    
  *                   
  *          3. TIM Input Capture management: this group includes all needed 
  *             functions to configure the Capture/Compare unit used in 
  *             Input Capture mode:
  *                   - Configure each channel in input capture mode
  *                   - Configure Channel1/2 in PWM Input mode
  *                   - Set the Input Capture Prescaler
  *                   - Get the Capture/Compare values      
  *                   
  *          4. Advanced-control timers (TIM1 and TIM8) specific features
  *                   - Configures the Break input, dead time, Lock level, the OSSI,
  *                      the OSSR State and the AOE(automatic output enable)
  *                   - Enable/Disable the TIM peripheral Main Outputs
  *                   - Select the Commutation event
  *                   - Set/Reset the Capture Compare Preload Control bit
  *                              
  *          5. TIM interrupts, DMA and flags management
  *                   - Enable/Disable interrupt sources
  *                   - Get flags status
  *                   - Clear flags/ Pending bits
  *                   - Enable/Disable DMA requests 
  *                   - Configure DMA burst mode
  *                   - Select CaptureCompare DMA request  
  *              
  *          6. TIM clocks management: this group includes all needed functions 
  *             to configure the clock controller unit:
  *                   - Select internal/External clock
  *                   - Select the external clock mode: ETR(Mode1/Mode2), TIx or ITRx
  *         
  *          7. TIM synchronization management: this group includes all needed 
  *             functions to configure the Synchronization unit:
  *                   - Select Input Trigger  
  *                   - Select Output Trigger  
  *                   - Select Master Slave Mode 
  *                   - ETR Configuration when used as external trigger   
  *     
  *          8. TIM specific interface management, this group includes all 
  *             needed functions to use the specific TIM interface:
  *                   - Encoder Interface Configuration
  *                   - Select Hall Sensor   
  *         
  *          9. TIM specific remapping management includes the Remapping 
  *             configuration of specific timers               
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
#include "stm32f4xx_tim.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup TIM 
  * @brief TIM driver modules
  * @{
  */

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/

/* ---------------------- TIM registers bit mask ------------------------ */
#define SMCR_ETR_MASK      ((uint16_t)0x00FF) 
#define CCMR_OFFSET        ((uint16_t)0x0018)
#define CCER_CCE_SET       ((uint16_t)0x0001)  
#define	CCER_CCNE_SET      ((uint16_t)0x0004) 
#define CCMR_OC13M_MASK    ((uint16_t)0xFF8F)
#define CCMR_OC24M_MASK    ((uint16_t)0x8FFF) 

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
static void TI1_Config(TIM_TypeDef* TIMx, uint16_t TIM_ICPolarity, uint16_t TIM_ICSelection,
                       uint16_t TIM_ICFilter);
static void TI2_Config(TIM_TypeDef* TIMx, uint16_t TIM_ICPolarity, uint16_t TIM_ICSelection,
                       uint16_t TIM_ICFilter);
static void TI3_Config(TIM_TypeDef* TIMx, uint16_t TIM_ICPolarity, uint16_t TIM_ICSelection,
                       uint16_t TIM_ICFilter);
static void TI4_Config(TIM_TypeDef* TIMx, uint16_t TIM_ICPolarity, uint16_t TIM_ICSelection,
                       uint16_t TIM_ICFilter);

/* Private functions ---------------------------------------------------------*/

/** @defgroup TIM_Private_Functions
  * @{
  */

/** @defgroup TIM_Group1 TimeBase management functions
 *  @brief   TimeBase management functions 
 *
@verbatim   
 ===============================================================================
                       TimeBase management functions
 ===============================================================================  
  
       ===================================================================      
              TIM Driver: how to use it in Timing(Time base) Mode
       =================================================================== 
       To use the Timer in Timing(Time base) mode, the following steps are mandatory:
       
       1. Enable TIM clock using RCC_APBxPeriphClockCmd(RCC_APBxPeriph_TIMx, ENABLE) function
                    
       2. Fill the TIM_TimeBaseInitStruct with the desired parameters.
       
       3. Call TIM_TimeBaseInit(TIMx, &TIM_TimeBaseInitStruct) to configure the Time Base unit
          with the corresponding configuration
          
       4. Enable the NVIC if you need to generate the update interrupt. 
          
       5. Enable the corresponding interrupt using the function TIM_ITConfig(TIMx, TIM_IT_Update) 
       
       6. Call the TIM_Cmd(ENABLE) function to enable the TIM counter.
             
       Note1: All other functions can be used separately to modify, if needed,
          a specific feature of the Timer. 

@endverbatim
  * @{
  */
  
/**
  * @brief  Deinitializes the TIMx peripheral registers to their default reset values.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @retval None

  */
void TIM_DeInit(TIM_TypeDef* TIMx)
{
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx)); 
 
  if (TIMx == TIM1)
  {
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_TIM1, ENABLE);
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_TIM1, DISABLE);  
  } 
  else if (TIMx == TIM2) 
  {     
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM2, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM2, DISABLE);
  }  
  else if (TIMx == TIM3)
  { 
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM3, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM3, DISABLE);
  }  
  else if (TIMx == TIM4)
  { 
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM4, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM4, DISABLE);
  }  
  else if (TIMx == TIM5)
  {      
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM5, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM5, DISABLE);
  }  
  else if (TIMx == TIM6)  
  {    
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM6, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM6, DISABLE);
  }  
  else if (TIMx == TIM7)
  {      
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM7, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM7, DISABLE);
  }  
  else if (TIMx == TIM8)
  {      
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_TIM8, ENABLE);
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_TIM8, DISABLE);  
  }  
  else if (TIMx == TIM9)
  {      
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_TIM9, ENABLE);
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_TIM9, DISABLE);  
   }  
  else if (TIMx == TIM10)
  {      
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_TIM10, ENABLE);
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_TIM10, DISABLE);  
  }  
  else if (TIMx == TIM11) 
  {     
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_TIM11, ENABLE);
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_TIM11, DISABLE);  
  }  
  else if (TIMx == TIM12)
  {      
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM12, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM12, DISABLE);  
  }  
  else if (TIMx == TIM13) 
  {       
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM13, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM13, DISABLE);  
  }  
  else
  { 
    if (TIMx == TIM14) 
    {     
      RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM14, ENABLE);
      RCC_APB1PeriphResetCmd(RCC_APB1Periph_TIM14, DISABLE); 
    }   
  }
}

/**
  * @brief  Initializes the TIMx Time Base Unit peripheral according to 
  *         the specified parameters in the TIM_TimeBaseInitStruct.
  * @param  TIMx: where x can be  1 to 14 to select the TIM peripheral.
  * @param  TIM_TimeBaseInitStruct: pointer to a TIM_TimeBaseInitTypeDef structure
  *         that contains the configuration information for the specified TIM peripheral.
  * @retval None
  */
void TIM_TimeBaseInit(TIM_TypeDef* TIMx, TIM_TimeBaseInitTypeDef* TIM_TimeBaseInitStruct)
{
  uint16_t tmpcr1 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx)); 
  assert_param(IS_TIM_COUNTER_MODE(TIM_TimeBaseInitStruct->TIM_CounterMode));
  assert_param(IS_TIM_CKD_DIV(TIM_TimeBaseInitStruct->TIM_ClockDivision));

  tmpcr1 = TIMx->CR1;  

  if((TIMx == TIM1) || (TIMx == TIM8)||
     (TIMx == TIM2) || (TIMx == TIM3)||
     (TIMx == TIM4) || (TIMx == TIM5)) 
  {
    /* Select the Counter Mode */
    tmpcr1 &= (uint16_t)(~(TIM_CR1_DIR | TIM_CR1_CMS));
    tmpcr1 |= (uint32_t)TIM_TimeBaseInitStruct->TIM_CounterMode;
  }
 
  if((TIMx != TIM6) && (TIMx != TIM7))
  {
    /* Set the clock division */
    tmpcr1 &=  (uint16_t)(~TIM_CR1_CKD);
    tmpcr1 |= (uint32_t)TIM_TimeBaseInitStruct->TIM_ClockDivision;
  }

  TIMx->CR1 = tmpcr1;

  /* Set the Autoreload value */
  TIMx->ARR = TIM_TimeBaseInitStruct->TIM_Period ;
 
  /* Set the Prescaler value */
  TIMx->PSC = TIM_TimeBaseInitStruct->TIM_Prescaler;
    
  if ((TIMx == TIM1) || (TIMx == TIM8))  
  {
    /* Set the Repetition Counter value */
    TIMx->RCR = TIM_TimeBaseInitStruct->TIM_RepetitionCounter;
  }

  /* Generate an update event to reload the Prescaler 
     and the repetition counter(only for TIM1 and TIM8) value immediatly */
  TIMx->EGR = TIM_PSCReloadMode_Immediate;          
}

/**
  * @brief  Fills each TIM_TimeBaseInitStruct member with its default value.
  * @param  TIM_TimeBaseInitStruct : pointer to a TIM_TimeBaseInitTypeDef
  *         structure which will be initialized.
  * @retval None
  */
void TIM_TimeBaseStructInit(TIM_TimeBaseInitTypeDef* TIM_TimeBaseInitStruct)
{
  /* Set the default configuration */
  TIM_TimeBaseInitStruct->TIM_Period = 0xFFFFFFFF;
  TIM_TimeBaseInitStruct->TIM_Prescaler = 0x0000;
  TIM_TimeBaseInitStruct->TIM_ClockDivision = TIM_CKD_DIV1;
  TIM_TimeBaseInitStruct->TIM_CounterMode = TIM_CounterMode_Up;
  TIM_TimeBaseInitStruct->TIM_RepetitionCounter = 0x0000;
}

/**
  * @brief  Configures the TIMx Prescaler.
  * @param  TIMx: where x can be  1 to 14 to select the TIM peripheral.
  * @param  Prescaler: specifies the Prescaler Register value
  * @param  TIM_PSCReloadMode: specifies the TIM Prescaler Reload mode
  *          This parameter can be one of the following values:
  *            @arg TIM_PSCReloadMode_Update: The Prescaler is loaded at the update event.
  *            @arg TIM_PSCReloadMode_Immediate: The Prescaler is loaded immediatly.
  * @retval None
  */
void TIM_PrescalerConfig(TIM_TypeDef* TIMx, uint16_t Prescaler, uint16_t TIM_PSCReloadMode)
{
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));
  assert_param(IS_TIM_PRESCALER_RELOAD(TIM_PSCReloadMode));
  /* Set the Prescaler value */
  TIMx->PSC = Prescaler;
  /* Set or reset the UG Bit */
  TIMx->EGR = TIM_PSCReloadMode;
}

/**
  * @brief  Specifies the TIMx Counter Mode to be used.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_CounterMode: specifies the Counter Mode to be used
  *          This parameter can be one of the following values:
  *            @arg TIM_CounterMode_Up: TIM Up Counting Mode
  *            @arg TIM_CounterMode_Down: TIM Down Counting Mode
  *            @arg TIM_CounterMode_CenterAligned1: TIM Center Aligned Mode1
  *            @arg TIM_CounterMode_CenterAligned2: TIM Center Aligned Mode2
  *            @arg TIM_CounterMode_CenterAligned3: TIM Center Aligned Mode3
  * @retval None
  */
void TIM_CounterModeConfig(TIM_TypeDef* TIMx, uint16_t TIM_CounterMode)
{
  uint16_t tmpcr1 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_COUNTER_MODE(TIM_CounterMode));

  tmpcr1 = TIMx->CR1;

  /* Reset the CMS and DIR Bits */
  tmpcr1 &= (uint16_t)~(TIM_CR1_DIR | TIM_CR1_CMS);

  /* Set the Counter Mode */
  tmpcr1 |= TIM_CounterMode;

  /* Write to TIMx CR1 register */
  TIMx->CR1 = tmpcr1;
}

/**
  * @brief  Sets the TIMx Counter Register value
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @param  Counter: specifies the Counter register new value.
  * @retval None
  */
void TIM_SetCounter(TIM_TypeDef* TIMx, uint32_t Counter)
{
  /* Check the parameters */
   assert_param(IS_TIM_ALL_PERIPH(TIMx));

  /* Set the Counter Register value */
  TIMx->CNT = Counter;
}

/**
  * @brief  Sets the TIMx Autoreload Register value
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @param  Autoreload: specifies the Autoreload register new value.
  * @retval None
  */
void TIM_SetAutoreload(TIM_TypeDef* TIMx, uint32_t Autoreload)
{
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));
  
  /* Set the Autoreload Register value */
  TIMx->ARR = Autoreload;
}

/**
  * @brief  Gets the TIMx Counter value.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @retval Counter Register value
  */
uint32_t TIM_GetCounter(TIM_TypeDef* TIMx)
{
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));

  /* Get the Counter Register value */
  return TIMx->CNT;
}

/**
  * @brief  Gets the TIMx Prescaler value.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @retval Prescaler Register value.
  */
uint16_t TIM_GetPrescaler(TIM_TypeDef* TIMx)
{
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));

  /* Get the Prescaler Register value */
  return TIMx->PSC;
}

/**
  * @brief  Enables or Disables the TIMx Update event.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @param  NewState: new state of the TIMx UDIS bit
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void TIM_UpdateDisableConfig(TIM_TypeDef* TIMx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Set the Update Disable Bit */
    TIMx->CR1 |= TIM_CR1_UDIS;
  }
  else
  {
    /* Reset the Update Disable Bit */
    TIMx->CR1 &= (uint16_t)~TIM_CR1_UDIS;
  }
}

/**
  * @brief  Configures the TIMx Update Request Interrupt source.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @param  TIM_UpdateSource: specifies the Update source.
  *          This parameter can be one of the following values:
  *            @arg TIM_UpdateSource_Global: Source of update is the counter
  *                 overflow/underflow or the setting of UG bit, or an update
  *                 generation through the slave mode controller.
  *            @arg TIM_UpdateSource_Regular: Source of update is counter overflow/underflow.
  * @retval None
  */
void TIM_UpdateRequestConfig(TIM_TypeDef* TIMx, uint16_t TIM_UpdateSource)
{
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));
  assert_param(IS_TIM_UPDATE_SOURCE(TIM_UpdateSource));

  if (TIM_UpdateSource != TIM_UpdateSource_Global)
  {
    /* Set the URS Bit */
    TIMx->CR1 |= TIM_CR1_URS;
  }
  else
  {
    /* Reset the URS Bit */
    TIMx->CR1 &= (uint16_t)~TIM_CR1_URS;
  }
}

/**
  * @brief  Enables or disables TIMx peripheral Preload register on ARR.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @param  NewState: new state of the TIMx peripheral Preload register
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void TIM_ARRPreloadConfig(TIM_TypeDef* TIMx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Set the ARR Preload Bit */
    TIMx->CR1 |= TIM_CR1_ARPE;
  }
  else
  {
    /* Reset the ARR Preload Bit */
    TIMx->CR1 &= (uint16_t)~TIM_CR1_ARPE;
  }
}

/**
  * @brief  Selects the TIMx's One Pulse Mode.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @param  TIM_OPMode: specifies the OPM Mode to be used.
  *          This parameter can be one of the following values:
  *            @arg TIM_OPMode_Single
  *            @arg TIM_OPMode_Repetitive
  * @retval None
  */
void TIM_SelectOnePulseMode(TIM_TypeDef* TIMx, uint16_t TIM_OPMode)
{
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));
  assert_param(IS_TIM_OPM_MODE(TIM_OPMode));

  /* Reset the OPM Bit */
  TIMx->CR1 &= (uint16_t)~TIM_CR1_OPM;

  /* Configure the OPM Mode */
  TIMx->CR1 |= TIM_OPMode;
}

/**
  * @brief  Sets the TIMx Clock Division value.
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  TIM_CKD: specifies the clock division value.
  *          This parameter can be one of the following value:
  *            @arg TIM_CKD_DIV1: TDTS = Tck_tim
  *            @arg TIM_CKD_DIV2: TDTS = 2*Tck_tim
  *            @arg TIM_CKD_DIV4: TDTS = 4*Tck_tim
  * @retval None
  */
void TIM_SetClockDivision(TIM_TypeDef* TIMx, uint16_t TIM_CKD)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));
  assert_param(IS_TIM_CKD_DIV(TIM_CKD));

  /* Reset the CKD Bits */
  TIMx->CR1 &= (uint16_t)(~TIM_CR1_CKD);

  /* Set the CKD value */
  TIMx->CR1 |= TIM_CKD;
}

/**
  * @brief  Enables or disables the specified TIM peripheral.
  * @param  TIMx: where x can be 1 to 14 to select the TIMx peripheral.
  * @param  NewState: new state of the TIMx peripheral.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void TIM_Cmd(TIM_TypeDef* TIMx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx)); 
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the TIM Counter */
    TIMx->CR1 |= TIM_CR1_CEN;
  }
  else
  {
    /* Disable the TIM Counter */
    TIMx->CR1 &= (uint16_t)~TIM_CR1_CEN;
  }
}
/**
  * @}
  */

/** @defgroup TIM_Group2 Output Compare management functions
 *  @brief    Output Compare management functions 
 *
@verbatim   
 ===============================================================================
                        Output Compare management functions
 ===============================================================================  
   
       ===================================================================      
              TIM Driver: how to use it in Output Compare Mode
       =================================================================== 
       To use the Timer in Output Compare mode, the following steps are mandatory:
       
       1. Enable TIM clock using RCC_APBxPeriphClockCmd(RCC_APBxPeriph_TIMx, ENABLE) function
       
       2. Configure the TIM pins by configuring the corresponding GPIO pins
       
       2. Configure the Time base unit as described in the first part of this driver, 
          if needed, else the Timer will run with the default configuration:
          - Autoreload value = 0xFFFF
          - Prescaler value = 0x0000
          - Counter mode = Up counting
          - Clock Division = TIM_CKD_DIV1
          
       3. Fill the TIM_OCInitStruct with the desired parameters including:
          - The TIM Output Compare mode: TIM_OCMode
          - TIM Output State: TIM_OutputState
          - TIM Pulse value: TIM_Pulse
          - TIM Output Compare Polarity : TIM_OCPolarity
       
       4. Call TIM_OCxInit(TIMx, &TIM_OCInitStruct) to configure the desired channel with the 
          corresponding configuration
       
       5. Call the TIM_Cmd(ENABLE) function to enable the TIM counter.
       
       Note1: All other functions can be used separately to modify, if needed,
              a specific feature of the Timer. 
          
       Note2: In case of PWM mode, this function is mandatory:
              TIM_OCxPreloadConfig(TIMx, TIM_OCPreload_ENABLE); 
              
       Note3: If the corresponding interrupt or DMA request are needed, the user should:
                1. Enable the NVIC (or the DMA) to use the TIM interrupts (or DMA requests). 
                2. Enable the corresponding interrupt (or DMA request) using the function 
                   TIM_ITConfig(TIMx, TIM_IT_CCx) (or TIM_DMA_Cmd(TIMx, TIM_DMA_CCx))   

@endverbatim
  * @{
  */

/**
  * @brief  Initializes the TIMx Channel1 according to the specified parameters in
  *         the TIM_OCInitStruct.
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  TIM_OCInitStruct: pointer to a TIM_OCInitTypeDef structure that contains
  *         the configuration information for the specified TIM peripheral.
  * @retval None
  */
void TIM_OC1Init(TIM_TypeDef* TIMx, TIM_OCInitTypeDef* TIM_OCInitStruct)
{
  uint16_t tmpccmrx = 0, tmpccer = 0, tmpcr2 = 0;
   
  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx)); 
  assert_param(IS_TIM_OC_MODE(TIM_OCInitStruct->TIM_OCMode));
  assert_param(IS_TIM_OUTPUT_STATE(TIM_OCInitStruct->TIM_OutputState));
  assert_param(IS_TIM_OC_POLARITY(TIM_OCInitStruct->TIM_OCPolarity));   

  /* Disable the Channel 1: Reset the CC1E Bit */
  TIMx->CCER &= (uint16_t)~TIM_CCER_CC1E;
  
  /* Get the TIMx CCER register value */
  tmpccer = TIMx->CCER;
  /* Get the TIMx CR2 register value */
  tmpcr2 =  TIMx->CR2;
  
  /* Get the TIMx CCMR1 register value */
  tmpccmrx = TIMx->CCMR1;
    
  /* Reset the Output Compare Mode Bits */
  tmpccmrx &= (uint16_t)~TIM_CCMR1_OC1M;
  tmpccmrx &= (uint16_t)~TIM_CCMR1_CC1S;
  /* Select the Output Compare Mode */
  tmpccmrx |= TIM_OCInitStruct->TIM_OCMode;
  
  /* Reset the Output Polarity level */
  tmpccer &= (uint16_t)~TIM_CCER_CC1P;
  /* Set the Output Compare Polarity */
  tmpccer |= TIM_OCInitStruct->TIM_OCPolarity;
  
  /* Set the Output State */
  tmpccer |= TIM_OCInitStruct->TIM_OutputState;
    
  if((TIMx == TIM1) || (TIMx == TIM8))
  {
    assert_param(IS_TIM_OUTPUTN_STATE(TIM_OCInitStruct->TIM_OutputNState));
    assert_param(IS_TIM_OCN_POLARITY(TIM_OCInitStruct->TIM_OCNPolarity));
    assert_param(IS_TIM_OCNIDLE_STATE(TIM_OCInitStruct->TIM_OCNIdleState));
    assert_param(IS_TIM_OCIDLE_STATE(TIM_OCInitStruct->TIM_OCIdleState));
    
    /* Reset the Output N Polarity level */
    tmpccer &= (uint16_t)~TIM_CCER_CC1NP;
    /* Set the Output N Polarity */
    tmpccer |= TIM_OCInitStruct->TIM_OCNPolarity;
    /* Reset the Output N State */
    tmpccer &= (uint16_t)~TIM_CCER_CC1NE;
    
    /* Set the Output N State */
    tmpccer |= TIM_OCInitStruct->TIM_OutputNState;
    /* Reset the Output Compare and Output Compare N IDLE State */
    tmpcr2 &= (uint16_t)~TIM_CR2_OIS1;
    tmpcr2 &= (uint16_t)~TIM_CR2_OIS1N;
    /* Set the Output Idle state */
    tmpcr2 |= TIM_OCInitStruct->TIM_OCIdleState;
    /* Set the Output N Idle state */
    tmpcr2 |= TIM_OCInitStruct->TIM_OCNIdleState;
  }
  /* Write to TIMx CR2 */
  TIMx->CR2 = tmpcr2;
  
  /* Write to TIMx CCMR1 */
  TIMx->CCMR1 = tmpccmrx;
  
  /* Set the Capture Compare Register value */
  TIMx->CCR1 = TIM_OCInitStruct->TIM_Pulse;
  
  /* Write to TIMx CCER */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Initializes the TIMx Channel2 according to the specified parameters 
  *         in the TIM_OCInitStruct.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  TIM_OCInitStruct: pointer to a TIM_OCInitTypeDef structure that contains
  *         the configuration information for the specified TIM peripheral.
  * @retval None
  */
void TIM_OC2Init(TIM_TypeDef* TIMx, TIM_OCInitTypeDef* TIM_OCInitStruct)
{
  uint16_t tmpccmrx = 0, tmpccer = 0, tmpcr2 = 0;
   
  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx)); 
  assert_param(IS_TIM_OC_MODE(TIM_OCInitStruct->TIM_OCMode));
  assert_param(IS_TIM_OUTPUT_STATE(TIM_OCInitStruct->TIM_OutputState));
  assert_param(IS_TIM_OC_POLARITY(TIM_OCInitStruct->TIM_OCPolarity));   

  /* Disable the Channel 2: Reset the CC2E Bit */
  TIMx->CCER &= (uint16_t)~TIM_CCER_CC2E;
  
  /* Get the TIMx CCER register value */  
  tmpccer = TIMx->CCER;
  /* Get the TIMx CR2 register value */
  tmpcr2 =  TIMx->CR2;
  
  /* Get the TIMx CCMR1 register value */
  tmpccmrx = TIMx->CCMR1;
    
  /* Reset the Output Compare mode and Capture/Compare selection Bits */
  tmpccmrx &= (uint16_t)~TIM_CCMR1_OC2M;
  tmpccmrx &= (uint16_t)~TIM_CCMR1_CC2S;
  
  /* Select the Output Compare Mode */
  tmpccmrx |= (uint16_t)(TIM_OCInitStruct->TIM_OCMode << 8);
  
  /* Reset the Output Polarity level */
  tmpccer &= (uint16_t)~TIM_CCER_CC2P;
  /* Set the Output Compare Polarity */
  tmpccer |= (uint16_t)(TIM_OCInitStruct->TIM_OCPolarity << 4);
  
  /* Set the Output State */
  tmpccer |= (uint16_t)(TIM_OCInitStruct->TIM_OutputState << 4);
    
  if((TIMx == TIM1) || (TIMx == TIM8))
  {
    assert_param(IS_TIM_OUTPUTN_STATE(TIM_OCInitStruct->TIM_OutputNState));
    assert_param(IS_TIM_OCN_POLARITY(TIM_OCInitStruct->TIM_OCNPolarity));
    assert_param(IS_TIM_OCNIDLE_STATE(TIM_OCInitStruct->TIM_OCNIdleState));
    assert_param(IS_TIM_OCIDLE_STATE(TIM_OCInitStruct->TIM_OCIdleState));
    
    /* Reset the Output N Polarity level */
    tmpccer &= (uint16_t)~TIM_CCER_CC2NP;
    /* Set the Output N Polarity */
    tmpccer |= (uint16_t)(TIM_OCInitStruct->TIM_OCNPolarity << 4);
    /* Reset the Output N State */
    tmpccer &= (uint16_t)~TIM_CCER_CC2NE;
    
    /* Set the Output N State */
    tmpccer |= (uint16_t)(TIM_OCInitStruct->TIM_OutputNState << 4);
    /* Reset the Output Compare and Output Compare N IDLE State */
    tmpcr2 &= (uint16_t)~TIM_CR2_OIS2;
    tmpcr2 &= (uint16_t)~TIM_CR2_OIS2N;
    /* Set the Output Idle state */
    tmpcr2 |= (uint16_t)(TIM_OCInitStruct->TIM_OCIdleState << 2);
    /* Set the Output N Idle state */
    tmpcr2 |= (uint16_t)(TIM_OCInitStruct->TIM_OCNIdleState << 2);
  }
  /* Write to TIMx CR2 */
  TIMx->CR2 = tmpcr2;
  
  /* Write to TIMx CCMR1 */
  TIMx->CCMR1 = tmpccmrx;
  
  /* Set the Capture Compare Register value */
  TIMx->CCR2 = TIM_OCInitStruct->TIM_Pulse;
  
  /* Write to TIMx CCER */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Initializes the TIMx Channel3 according to the specified parameters
  *         in the TIM_OCInitStruct.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_OCInitStruct: pointer to a TIM_OCInitTypeDef structure that contains
  *         the configuration information for the specified TIM peripheral.
  * @retval None
  */
void TIM_OC3Init(TIM_TypeDef* TIMx, TIM_OCInitTypeDef* TIM_OCInitStruct)
{
  uint16_t tmpccmrx = 0, tmpccer = 0, tmpcr2 = 0;
   
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx)); 
  assert_param(IS_TIM_OC_MODE(TIM_OCInitStruct->TIM_OCMode));
  assert_param(IS_TIM_OUTPUT_STATE(TIM_OCInitStruct->TIM_OutputState));
  assert_param(IS_TIM_OC_POLARITY(TIM_OCInitStruct->TIM_OCPolarity));   

  /* Disable the Channel 3: Reset the CC2E Bit */
  TIMx->CCER &= (uint16_t)~TIM_CCER_CC3E;
  
  /* Get the TIMx CCER register value */
  tmpccer = TIMx->CCER;
  /* Get the TIMx CR2 register value */
  tmpcr2 =  TIMx->CR2;
  
  /* Get the TIMx CCMR2 register value */
  tmpccmrx = TIMx->CCMR2;
    
  /* Reset the Output Compare mode and Capture/Compare selection Bits */
  tmpccmrx &= (uint16_t)~TIM_CCMR2_OC3M;
  tmpccmrx &= (uint16_t)~TIM_CCMR2_CC3S;  
  /* Select the Output Compare Mode */
  tmpccmrx |= TIM_OCInitStruct->TIM_OCMode;
  
  /* Reset the Output Polarity level */
  tmpccer &= (uint16_t)~TIM_CCER_CC3P;
  /* Set the Output Compare Polarity */
  tmpccer |= (uint16_t)(TIM_OCInitStruct->TIM_OCPolarity << 8);
  
  /* Set the Output State */
  tmpccer |= (uint16_t)(TIM_OCInitStruct->TIM_OutputState << 8);
    
  if((TIMx == TIM1) || (TIMx == TIM8))
  {
    assert_param(IS_TIM_OUTPUTN_STATE(TIM_OCInitStruct->TIM_OutputNState));
    assert_param(IS_TIM_OCN_POLARITY(TIM_OCInitStruct->TIM_OCNPolarity));
    assert_param(IS_TIM_OCNIDLE_STATE(TIM_OCInitStruct->TIM_OCNIdleState));
    assert_param(IS_TIM_OCIDLE_STATE(TIM_OCInitStruct->TIM_OCIdleState));
    
    /* Reset the Output N Polarity level */
    tmpccer &= (uint16_t)~TIM_CCER_CC3NP;
    /* Set the Output N Polarity */
    tmpccer |= (uint16_t)(TIM_OCInitStruct->TIM_OCNPolarity << 8);
    /* Reset the Output N State */
    tmpccer &= (uint16_t)~TIM_CCER_CC3NE;
    
    /* Set the Output N State */
    tmpccer |= (uint16_t)(TIM_OCInitStruct->TIM_OutputNState << 8);
    /* Reset the Output Compare and Output Compare N IDLE State */
    tmpcr2 &= (uint16_t)~TIM_CR2_OIS3;
    tmpcr2 &= (uint16_t)~TIM_CR2_OIS3N;
    /* Set the Output Idle state */
    tmpcr2 |= (uint16_t)(TIM_OCInitStruct->TIM_OCIdleState << 4);
    /* Set the Output N Idle state */
    tmpcr2 |= (uint16_t)(TIM_OCInitStruct->TIM_OCNIdleState << 4);
  }
  /* Write to TIMx CR2 */
  TIMx->CR2 = tmpcr2;
  
  /* Write to TIMx CCMR2 */
  TIMx->CCMR2 = tmpccmrx;
  
  /* Set the Capture Compare Register value */
  TIMx->CCR3 = TIM_OCInitStruct->TIM_Pulse;
  
  /* Write to TIMx CCER */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Initializes the TIMx Channel4 according to the specified parameters
  *         in the TIM_OCInitStruct.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_OCInitStruct: pointer to a TIM_OCInitTypeDef structure that contains
  *         the configuration information for the specified TIM peripheral.
  * @retval None
  */
void TIM_OC4Init(TIM_TypeDef* TIMx, TIM_OCInitTypeDef* TIM_OCInitStruct)
{
  uint16_t tmpccmrx = 0, tmpccer = 0, tmpcr2 = 0;
   
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx)); 
  assert_param(IS_TIM_OC_MODE(TIM_OCInitStruct->TIM_OCMode));
  assert_param(IS_TIM_OUTPUT_STATE(TIM_OCInitStruct->TIM_OutputState));
  assert_param(IS_TIM_OC_POLARITY(TIM_OCInitStruct->TIM_OCPolarity));   

  /* Disable the Channel 4: Reset the CC4E Bit */
  TIMx->CCER &= (uint16_t)~TIM_CCER_CC4E;
  
  /* Get the TIMx CCER register value */
  tmpccer = TIMx->CCER;
  /* Get the TIMx CR2 register value */
  tmpcr2 =  TIMx->CR2;
  
  /* Get the TIMx CCMR2 register value */
  tmpccmrx = TIMx->CCMR2;
    
  /* Reset the Output Compare mode and Capture/Compare selection Bits */
  tmpccmrx &= (uint16_t)~TIM_CCMR2_OC4M;
  tmpccmrx &= (uint16_t)~TIM_CCMR2_CC4S;
  
  /* Select the Output Compare Mode */
  tmpccmrx |= (uint16_t)(TIM_OCInitStruct->TIM_OCMode << 8);
  
  /* Reset the Output Polarity level */
  tmpccer &= (uint16_t)~TIM_CCER_CC4P;
  /* Set the Output Compare Polarity */
  tmpccer |= (uint16_t)(TIM_OCInitStruct->TIM_OCPolarity << 12);
  
  /* Set the Output State */
  tmpccer |= (uint16_t)(TIM_OCInitStruct->TIM_OutputState << 12);
  
  if((TIMx == TIM1) || (TIMx == TIM8))
  {
    assert_param(IS_TIM_OCIDLE_STATE(TIM_OCInitStruct->TIM_OCIdleState));
    /* Reset the Output Compare IDLE State */
    tmpcr2 &=(uint16_t) ~TIM_CR2_OIS4;
    /* Set the Output Idle state */
    tmpcr2 |= (uint16_t)(TIM_OCInitStruct->TIM_OCIdleState << 6);
  }
  /* Write to TIMx CR2 */
  TIMx->CR2 = tmpcr2;
  
  /* Write to TIMx CCMR2 */  
  TIMx->CCMR2 = tmpccmrx;
    
  /* Set the Capture Compare Register value */
  TIMx->CCR4 = TIM_OCInitStruct->TIM_Pulse;
  
  /* Write to TIMx CCER */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Fills each TIM_OCInitStruct member with its default value.
  * @param  TIM_OCInitStruct: pointer to a TIM_OCInitTypeDef structure which will
  *         be initialized.
  * @retval None
  */
void TIM_OCStructInit(TIM_OCInitTypeDef* TIM_OCInitStruct)
{
  /* Set the default configuration */
  TIM_OCInitStruct->TIM_OCMode = TIM_OCMode_Timing;
  TIM_OCInitStruct->TIM_OutputState = TIM_OutputState_Disable;
  TIM_OCInitStruct->TIM_OutputNState = TIM_OutputNState_Disable;
  TIM_OCInitStruct->TIM_Pulse = 0x00000000;
  TIM_OCInitStruct->TIM_OCPolarity = TIM_OCPolarity_High;
  TIM_OCInitStruct->TIM_OCNPolarity = TIM_OCPolarity_High;
  TIM_OCInitStruct->TIM_OCIdleState = TIM_OCIdleState_Reset;
  TIM_OCInitStruct->TIM_OCNIdleState = TIM_OCNIdleState_Reset;
}

/**
  * @brief  Selects the TIM Output Compare Mode.
  * @note   This function disables the selected channel before changing the Output
  *         Compare Mode. If needed, user has to enable this channel using
  *         TIM_CCxCmd() and TIM_CCxNCmd() functions.
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  TIM_Channel: specifies the TIM Channel
  *          This parameter can be one of the following values:
  *            @arg TIM_Channel_1: TIM Channel 1
  *            @arg TIM_Channel_2: TIM Channel 2
  *            @arg TIM_Channel_3: TIM Channel 3
  *            @arg TIM_Channel_4: TIM Channel 4
  * @param  TIM_OCMode: specifies the TIM Output Compare Mode.
  *           This parameter can be one of the following values:
  *            @arg TIM_OCMode_Timing
  *            @arg TIM_OCMode_Active
  *            @arg TIM_OCMode_Toggle
  *            @arg TIM_OCMode_PWM1
  *            @arg TIM_OCMode_PWM2
  *            @arg TIM_ForcedAction_Active
  *            @arg TIM_ForcedAction_InActive
  * @retval None
  */
void TIM_SelectOCxM(TIM_TypeDef* TIMx, uint16_t TIM_Channel, uint16_t TIM_OCMode)
{
  uint32_t tmp = 0;
  uint16_t tmp1 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));
  assert_param(IS_TIM_CHANNEL(TIM_Channel));
  assert_param(IS_TIM_OCM(TIM_OCMode));

  tmp = (uint32_t) TIMx;
  tmp += CCMR_OFFSET;

  tmp1 = CCER_CCE_SET << (uint16_t)TIM_Channel;

  /* Disable the Channel: Reset the CCxE Bit */
  TIMx->CCER &= (uint16_t) ~tmp1;

  if((TIM_Channel == TIM_Channel_1) ||(TIM_Channel == TIM_Channel_3))
  {
    tmp += (TIM_Channel>>1);

    /* Reset the OCxM bits in the CCMRx register */
    *(__IO uint32_t *) tmp &= CCMR_OC13M_MASK;
   
    /* Configure the OCxM bits in the CCMRx register */
    *(__IO uint32_t *) tmp |= TIM_OCMode;
  }
  else
  {
    tmp += (uint16_t)(TIM_Channel - (uint16_t)4)>> (uint16_t)1;

    /* Reset the OCxM bits in the CCMRx register */
    *(__IO uint32_t *) tmp &= CCMR_OC24M_MASK;
    
    /* Configure the OCxM bits in the CCMRx register */
    *(__IO uint32_t *) tmp |= (uint16_t)(TIM_OCMode << 8);
  }
}

/**
  * @brief  Sets the TIMx Capture Compare1 Register value
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  Compare1: specifies the Capture Compare1 register new value.
  * @retval None
  */
void TIM_SetCompare1(TIM_TypeDef* TIMx, uint32_t Compare1)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));

  /* Set the Capture Compare1 Register value */
  TIMx->CCR1 = Compare1;
}

/**
  * @brief  Sets the TIMx Capture Compare2 Register value
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  Compare2: specifies the Capture Compare2 register new value.
  * @retval None
  */
void TIM_SetCompare2(TIM_TypeDef* TIMx, uint32_t Compare2)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));

  /* Set the Capture Compare2 Register value */
  TIMx->CCR2 = Compare2;
}

/**
  * @brief  Sets the TIMx Capture Compare3 Register value
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  Compare3: specifies the Capture Compare3 register new value.
  * @retval None
  */
void TIM_SetCompare3(TIM_TypeDef* TIMx, uint32_t Compare3)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));

  /* Set the Capture Compare3 Register value */
  TIMx->CCR3 = Compare3;
}

/**
  * @brief  Sets the TIMx Capture Compare4 Register value
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  Compare4: specifies the Capture Compare4 register new value.
  * @retval None
  */
void TIM_SetCompare4(TIM_TypeDef* TIMx, uint32_t Compare4)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));

  /* Set the Capture Compare4 Register value */
  TIMx->CCR4 = Compare4;
}

/**
  * @brief  Forces the TIMx output 1 waveform to active or inactive level.
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  TIM_ForcedAction: specifies the forced Action to be set to the output waveform.
  *          This parameter can be one of the following values:
  *            @arg TIM_ForcedAction_Active: Force active level on OC1REF
  *            @arg TIM_ForcedAction_InActive: Force inactive level on OC1REF.
  * @retval None
  */
void TIM_ForcedOC1Config(TIM_TypeDef* TIMx, uint16_t TIM_ForcedAction)
{
  uint16_t tmpccmr1 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));
  assert_param(IS_TIM_FORCED_ACTION(TIM_ForcedAction));
  tmpccmr1 = TIMx->CCMR1;

  /* Reset the OC1M Bits */
  tmpccmr1 &= (uint16_t)~TIM_CCMR1_OC1M;

  /* Configure The Forced output Mode */
  tmpccmr1 |= TIM_ForcedAction;

  /* Write to TIMx CCMR1 register */
  TIMx->CCMR1 = tmpccmr1;
}

/**
  * @brief  Forces the TIMx output 2 waveform to active or inactive level.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  TIM_ForcedAction: specifies the forced Action to be set to the output waveform.
  *          This parameter can be one of the following values:
  *            @arg TIM_ForcedAction_Active: Force active level on OC2REF
  *            @arg TIM_ForcedAction_InActive: Force inactive level on OC2REF.
  * @retval None
  */
void TIM_ForcedOC2Config(TIM_TypeDef* TIMx, uint16_t TIM_ForcedAction)
{
  uint16_t tmpccmr1 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));
  assert_param(IS_TIM_FORCED_ACTION(TIM_ForcedAction));
  tmpccmr1 = TIMx->CCMR1;

  /* Reset the OC2M Bits */
  tmpccmr1 &= (uint16_t)~TIM_CCMR1_OC2M;

  /* Configure The Forced output Mode */
  tmpccmr1 |= (uint16_t)(TIM_ForcedAction << 8);

  /* Write to TIMx CCMR1 register */
  TIMx->CCMR1 = tmpccmr1;
}

/**
  * @brief  Forces the TIMx output 3 waveform to active or inactive level.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_ForcedAction: specifies the forced Action to be set to the output waveform.
  *          This parameter can be one of the following values:
  *            @arg TIM_ForcedAction_Active: Force active level on OC3REF
  *            @arg TIM_ForcedAction_InActive: Force inactive level on OC3REF.
  * @retval None
  */
void TIM_ForcedOC3Config(TIM_TypeDef* TIMx, uint16_t TIM_ForcedAction)
{
  uint16_t tmpccmr2 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_FORCED_ACTION(TIM_ForcedAction));

  tmpccmr2 = TIMx->CCMR2;

  /* Reset the OC1M Bits */
  tmpccmr2 &= (uint16_t)~TIM_CCMR2_OC3M;

  /* Configure The Forced output Mode */
  tmpccmr2 |= TIM_ForcedAction;

  /* Write to TIMx CCMR2 register */
  TIMx->CCMR2 = tmpccmr2;
}

/**
  * @brief  Forces the TIMx output 4 waveform to active or inactive level.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_ForcedAction: specifies the forced Action to be set to the output waveform.
  *          This parameter can be one of the following values:
  *            @arg TIM_ForcedAction_Active: Force active level on OC4REF
  *            @arg TIM_ForcedAction_InActive: Force inactive level on OC4REF.
  * @retval None
  */
void TIM_ForcedOC4Config(TIM_TypeDef* TIMx, uint16_t TIM_ForcedAction)
{
  uint16_t tmpccmr2 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_FORCED_ACTION(TIM_ForcedAction));
  tmpccmr2 = TIMx->CCMR2;

  /* Reset the OC2M Bits */
  tmpccmr2 &= (uint16_t)~TIM_CCMR2_OC4M;

  /* Configure The Forced output Mode */
  tmpccmr2 |= (uint16_t)(TIM_ForcedAction << 8);

  /* Write to TIMx CCMR2 register */
  TIMx->CCMR2 = tmpccmr2;
}

/**
  * @brief  Enables or disables the TIMx peripheral Preload register on CCR1.
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  TIM_OCPreload: new state of the TIMx peripheral Preload register
  *          This parameter can be one of the following values:
  *            @arg TIM_OCPreload_Enable
  *            @arg TIM_OCPreload_Disable
  * @retval None
  */
void TIM_OC1PreloadConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCPreload)
{
  uint16_t tmpccmr1 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));
  assert_param(IS_TIM_OCPRELOAD_STATE(TIM_OCPreload));

  tmpccmr1 = TIMx->CCMR1;

  /* Reset the OC1PE Bit */
  tmpccmr1 &= (uint16_t)(~TIM_CCMR1_OC1PE);

  /* Enable or Disable the Output Compare Preload feature */
  tmpccmr1 |= TIM_OCPreload;

  /* Write to TIMx CCMR1 register */
  TIMx->CCMR1 = tmpccmr1;
}

/**
  * @brief  Enables or disables the TIMx peripheral Preload register on CCR2.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  TIM_OCPreload: new state of the TIMx peripheral Preload register
  *          This parameter can be one of the following values:
  *            @arg TIM_OCPreload_Enable
  *            @arg TIM_OCPreload_Disable
  * @retval None
  */
void TIM_OC2PreloadConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCPreload)
{
  uint16_t tmpccmr1 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));
  assert_param(IS_TIM_OCPRELOAD_STATE(TIM_OCPreload));

  tmpccmr1 = TIMx->CCMR1;

  /* Reset the OC2PE Bit */
  tmpccmr1 &= (uint16_t)(~TIM_CCMR1_OC2PE);

  /* Enable or Disable the Output Compare Preload feature */
  tmpccmr1 |= (uint16_t)(TIM_OCPreload << 8);

  /* Write to TIMx CCMR1 register */
  TIMx->CCMR1 = tmpccmr1;
}

/**
  * @brief  Enables or disables the TIMx peripheral Preload register on CCR3.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_OCPreload: new state of the TIMx peripheral Preload register
  *          This parameter can be one of the following values:
  *            @arg TIM_OCPreload_Enable
  *            @arg TIM_OCPreload_Disable
  * @retval None
  */
void TIM_OC3PreloadConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCPreload)
{
  uint16_t tmpccmr2 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_OCPRELOAD_STATE(TIM_OCPreload));

  tmpccmr2 = TIMx->CCMR2;

  /* Reset the OC3PE Bit */
  tmpccmr2 &= (uint16_t)(~TIM_CCMR2_OC3PE);

  /* Enable or Disable the Output Compare Preload feature */
  tmpccmr2 |= TIM_OCPreload;

  /* Write to TIMx CCMR2 register */
  TIMx->CCMR2 = tmpccmr2;
}

/**
  * @brief  Enables or disables the TIMx peripheral Preload register on CCR4.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_OCPreload: new state of the TIMx peripheral Preload register
  *          This parameter can be one of the following values:
  *            @arg TIM_OCPreload_Enable
  *            @arg TIM_OCPreload_Disable
  * @retval None
  */
void TIM_OC4PreloadConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCPreload)
{
  uint16_t tmpccmr2 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_OCPRELOAD_STATE(TIM_OCPreload));

  tmpccmr2 = TIMx->CCMR2;

  /* Reset the OC4PE Bit */
  tmpccmr2 &= (uint16_t)(~TIM_CCMR2_OC4PE);

  /* Enable or Disable the Output Compare Preload feature */
  tmpccmr2 |= (uint16_t)(TIM_OCPreload << 8);

  /* Write to TIMx CCMR2 register */
  TIMx->CCMR2 = tmpccmr2;
}

/**
  * @brief  Configures the TIMx Output Compare 1 Fast feature.
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  TIM_OCFast: new state of the Output Compare Fast Enable Bit.
  *          This parameter can be one of the following values:
  *            @arg TIM_OCFast_Enable: TIM output compare fast enable
  *            @arg TIM_OCFast_Disable: TIM output compare fast disable
  * @retval None
  */
void TIM_OC1FastConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCFast)
{
  uint16_t tmpccmr1 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));
  assert_param(IS_TIM_OCFAST_STATE(TIM_OCFast));

  /* Get the TIMx CCMR1 register value */
  tmpccmr1 = TIMx->CCMR1;

  /* Reset the OC1FE Bit */
  tmpccmr1 &= (uint16_t)~TIM_CCMR1_OC1FE;

  /* Enable or Disable the Output Compare Fast Bit */
  tmpccmr1 |= TIM_OCFast;

  /* Write to TIMx CCMR1 */
  TIMx->CCMR1 = tmpccmr1;
}

/**
  * @brief  Configures the TIMx Output Compare 2 Fast feature.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  TIM_OCFast: new state of the Output Compare Fast Enable Bit.
  *          This parameter can be one of the following values:
  *            @arg TIM_OCFast_Enable: TIM output compare fast enable
  *            @arg TIM_OCFast_Disable: TIM output compare fast disable
  * @retval None
  */
void TIM_OC2FastConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCFast)
{
  uint16_t tmpccmr1 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));
  assert_param(IS_TIM_OCFAST_STATE(TIM_OCFast));

  /* Get the TIMx CCMR1 register value */
  tmpccmr1 = TIMx->CCMR1;

  /* Reset the OC2FE Bit */
  tmpccmr1 &= (uint16_t)(~TIM_CCMR1_OC2FE);

  /* Enable or Disable the Output Compare Fast Bit */
  tmpccmr1 |= (uint16_t)(TIM_OCFast << 8);

  /* Write to TIMx CCMR1 */
  TIMx->CCMR1 = tmpccmr1;
}

/**
  * @brief  Configures the TIMx Output Compare 3 Fast feature.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_OCFast: new state of the Output Compare Fast Enable Bit.
  *          This parameter can be one of the following values:
  *            @arg TIM_OCFast_Enable: TIM output compare fast enable
  *            @arg TIM_OCFast_Disable: TIM output compare fast disable
  * @retval None
  */
void TIM_OC3FastConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCFast)
{
  uint16_t tmpccmr2 = 0;
  
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_OCFAST_STATE(TIM_OCFast));

  /* Get the TIMx CCMR2 register value */
  tmpccmr2 = TIMx->CCMR2;

  /* Reset the OC3FE Bit */
  tmpccmr2 &= (uint16_t)~TIM_CCMR2_OC3FE;

  /* Enable or Disable the Output Compare Fast Bit */
  tmpccmr2 |= TIM_OCFast;

  /* Write to TIMx CCMR2 */
  TIMx->CCMR2 = tmpccmr2;
}

/**
  * @brief  Configures the TIMx Output Compare 4 Fast feature.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_OCFast: new state of the Output Compare Fast Enable Bit.
  *          This parameter can be one of the following values:
  *            @arg TIM_OCFast_Enable: TIM output compare fast enable
  *            @arg TIM_OCFast_Disable: TIM output compare fast disable
  * @retval None
  */
void TIM_OC4FastConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCFast)
{
  uint16_t tmpccmr2 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_OCFAST_STATE(TIM_OCFast));

  /* Get the TIMx CCMR2 register value */
  tmpccmr2 = TIMx->CCMR2;

  /* Reset the OC4FE Bit */
  tmpccmr2 &= (uint16_t)(~TIM_CCMR2_OC4FE);

  /* Enable or Disable the Output Compare Fast Bit */
  tmpccmr2 |= (uint16_t)(TIM_OCFast << 8);

  /* Write to TIMx CCMR2 */
  TIMx->CCMR2 = tmpccmr2;
}

/**
  * @brief  Clears or safeguards the OCREF1 signal on an external event
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  TIM_OCClear: new state of the Output Compare Clear Enable Bit.
  *          This parameter can be one of the following values:
  *            @arg TIM_OCClear_Enable: TIM Output clear enable
  *            @arg TIM_OCClear_Disable: TIM Output clear disable
  * @retval None
  */
void TIM_ClearOC1Ref(TIM_TypeDef* TIMx, uint16_t TIM_OCClear)
{
  uint16_t tmpccmr1 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));
  assert_param(IS_TIM_OCCLEAR_STATE(TIM_OCClear));

  tmpccmr1 = TIMx->CCMR1;

  /* Reset the OC1CE Bit */
  tmpccmr1 &= (uint16_t)~TIM_CCMR1_OC1CE;

  /* Enable or Disable the Output Compare Clear Bit */
  tmpccmr1 |= TIM_OCClear;

  /* Write to TIMx CCMR1 register */
  TIMx->CCMR1 = tmpccmr1;
}

/**
  * @brief  Clears or safeguards the OCREF2 signal on an external event
  * @param  TIMx: where x can be  1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  TIM_OCClear: new state of the Output Compare Clear Enable Bit.
  *          This parameter can be one of the following values:
  *            @arg TIM_OCClear_Enable: TIM Output clear enable
  *            @arg TIM_OCClear_Disable: TIM Output clear disable
  * @retval None
  */
void TIM_ClearOC2Ref(TIM_TypeDef* TIMx, uint16_t TIM_OCClear)
{
  uint16_t tmpccmr1 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));
  assert_param(IS_TIM_OCCLEAR_STATE(TIM_OCClear));

  tmpccmr1 = TIMx->CCMR1;

  /* Reset the OC2CE Bit */
  tmpccmr1 &= (uint16_t)~TIM_CCMR1_OC2CE;

  /* Enable or Disable the Output Compare Clear Bit */
  tmpccmr1 |= (uint16_t)(TIM_OCClear << 8);

  /* Write to TIMx CCMR1 register */
  TIMx->CCMR1 = tmpccmr1;
}

/**
  * @brief  Clears or safeguards the OCREF3 signal on an external event
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_OCClear: new state of the Output Compare Clear Enable Bit.
  *          This parameter can be one of the following values:
  *            @arg TIM_OCClear_Enable: TIM Output clear enable
  *            @arg TIM_OCClear_Disable: TIM Output clear disable
  * @retval None
  */
void TIM_ClearOC3Ref(TIM_TypeDef* TIMx, uint16_t TIM_OCClear)
{
  uint16_t tmpccmr2 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_OCCLEAR_STATE(TIM_OCClear));

  tmpccmr2 = TIMx->CCMR2;

  /* Reset the OC3CE Bit */
  tmpccmr2 &= (uint16_t)~TIM_CCMR2_OC3CE;

  /* Enable or Disable the Output Compare Clear Bit */
  tmpccmr2 |= TIM_OCClear;

  /* Write to TIMx CCMR2 register */
  TIMx->CCMR2 = tmpccmr2;
}

/**
  * @brief  Clears or safeguards the OCREF4 signal on an external event
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_OCClear: new state of the Output Compare Clear Enable Bit.
  *          This parameter can be one of the following values:
  *            @arg TIM_OCClear_Enable: TIM Output clear enable
  *            @arg TIM_OCClear_Disable: TIM Output clear disable
  * @retval None
  */
void TIM_ClearOC4Ref(TIM_TypeDef* TIMx, uint16_t TIM_OCClear)
{
  uint16_t tmpccmr2 = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_OCCLEAR_STATE(TIM_OCClear));

  tmpccmr2 = TIMx->CCMR2;

  /* Reset the OC4CE Bit */
  tmpccmr2 &= (uint16_t)~TIM_CCMR2_OC4CE;

  /* Enable or Disable the Output Compare Clear Bit */
  tmpccmr2 |= (uint16_t)(TIM_OCClear << 8);

  /* Write to TIMx CCMR2 register */
  TIMx->CCMR2 = tmpccmr2;
}

/**
  * @brief  Configures the TIMx channel 1 polarity.
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  TIM_OCPolarity: specifies the OC1 Polarity
  *          This parameter can be one of the following values:
  *            @arg TIM_OCPolarity_High: Output Compare active high
  *            @arg TIM_OCPolarity_Low: Output Compare active low
  * @retval None
  */
void TIM_OC1PolarityConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCPolarity)
{
  uint16_t tmpccer = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));
  assert_param(IS_TIM_OC_POLARITY(TIM_OCPolarity));

  tmpccer = TIMx->CCER;

  /* Set or Reset the CC1P Bit */
  tmpccer &= (uint16_t)(~TIM_CCER_CC1P);
  tmpccer |= TIM_OCPolarity;

  /* Write to TIMx CCER register */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Configures the TIMx Channel 1N polarity.
  * @param  TIMx: where x can be 1 or 8 to select the TIM peripheral.
  * @param  TIM_OCNPolarity: specifies the OC1N Polarity
  *          This parameter can be one of the following values:
  *            @arg TIM_OCNPolarity_High: Output Compare active high
  *            @arg TIM_OCNPolarity_Low: Output Compare active low
  * @retval None
  */
void TIM_OC1NPolarityConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCNPolarity)
{
  uint16_t tmpccer = 0;
  /* Check the parameters */
  assert_param(IS_TIM_LIST4_PERIPH(TIMx));
  assert_param(IS_TIM_OCN_POLARITY(TIM_OCNPolarity));
   
  tmpccer = TIMx->CCER;

  /* Set or Reset the CC1NP Bit */
  tmpccer &= (uint16_t)~TIM_CCER_CC1NP;
  tmpccer |= TIM_OCNPolarity;

  /* Write to TIMx CCER register */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Configures the TIMx channel 2 polarity.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  TIM_OCPolarity: specifies the OC2 Polarity
  *          This parameter can be one of the following values:
  *            @arg TIM_OCPolarity_High: Output Compare active high
  *            @arg TIM_OCPolarity_Low: Output Compare active low
  * @retval None
  */
void TIM_OC2PolarityConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCPolarity)
{
  uint16_t tmpccer = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));
  assert_param(IS_TIM_OC_POLARITY(TIM_OCPolarity));

  tmpccer = TIMx->CCER;

  /* Set or Reset the CC2P Bit */
  tmpccer &= (uint16_t)(~TIM_CCER_CC2P);
  tmpccer |= (uint16_t)(TIM_OCPolarity << 4);

  /* Write to TIMx CCER register */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Configures the TIMx Channel 2N polarity.
  * @param  TIMx: where x can be 1 or 8 to select the TIM peripheral.
  * @param  TIM_OCNPolarity: specifies the OC2N Polarity
  *          This parameter can be one of the following values:
  *            @arg TIM_OCNPolarity_High: Output Compare active high
  *            @arg TIM_OCNPolarity_Low: Output Compare active low
  * @retval None
  */
void TIM_OC2NPolarityConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCNPolarity)
{
  uint16_t tmpccer = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST4_PERIPH(TIMx));
  assert_param(IS_TIM_OCN_POLARITY(TIM_OCNPolarity));
  
  tmpccer = TIMx->CCER;

  /* Set or Reset the CC2NP Bit */
  tmpccer &= (uint16_t)~TIM_CCER_CC2NP;
  tmpccer |= (uint16_t)(TIM_OCNPolarity << 4);

  /* Write to TIMx CCER register */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Configures the TIMx channel 3 polarity.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_OCPolarity: specifies the OC3 Polarity
  *          This parameter can be one of the following values:
  *            @arg TIM_OCPolarity_High: Output Compare active high
  *            @arg TIM_OCPolarity_Low: Output Compare active low
  * @retval None
  */
void TIM_OC3PolarityConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCPolarity)
{
  uint16_t tmpccer = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_OC_POLARITY(TIM_OCPolarity));

  tmpccer = TIMx->CCER;

  /* Set or Reset the CC3P Bit */
  tmpccer &= (uint16_t)~TIM_CCER_CC3P;
  tmpccer |= (uint16_t)(TIM_OCPolarity << 8);

  /* Write to TIMx CCER register */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Configures the TIMx Channel 3N polarity.
  * @param  TIMx: where x can be 1 or 8 to select the TIM peripheral.
  * @param  TIM_OCNPolarity: specifies the OC3N Polarity
  *          This parameter can be one of the following values:
  *            @arg TIM_OCNPolarity_High: Output Compare active high
  *            @arg TIM_OCNPolarity_Low: Output Compare active low
  * @retval None
  */
void TIM_OC3NPolarityConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCNPolarity)
{
  uint16_t tmpccer = 0;
 
  /* Check the parameters */
  assert_param(IS_TIM_LIST4_PERIPH(TIMx));
  assert_param(IS_TIM_OCN_POLARITY(TIM_OCNPolarity));
    
  tmpccer = TIMx->CCER;

  /* Set or Reset the CC3NP Bit */
  tmpccer &= (uint16_t)~TIM_CCER_CC3NP;
  tmpccer |= (uint16_t)(TIM_OCNPolarity << 8);

  /* Write to TIMx CCER register */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Configures the TIMx channel 4 polarity.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_OCPolarity: specifies the OC4 Polarity
  *          This parameter can be one of the following values:
  *            @arg TIM_OCPolarity_High: Output Compare active high
  *            @arg TIM_OCPolarity_Low: Output Compare active low
  * @retval None
  */
void TIM_OC4PolarityConfig(TIM_TypeDef* TIMx, uint16_t TIM_OCPolarity)
{
  uint16_t tmpccer = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_OC_POLARITY(TIM_OCPolarity));

  tmpccer = TIMx->CCER;

  /* Set or Reset the CC4P Bit */
  tmpccer &= (uint16_t)~TIM_CCER_CC4P;
  tmpccer |= (uint16_t)(TIM_OCPolarity << 12);

  /* Write to TIMx CCER register */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Enables or disables the TIM Capture Compare Channel x.
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  TIM_Channel: specifies the TIM Channel
  *          This parameter can be one of the following values:
  *            @arg TIM_Channel_1: TIM Channel 1
  *            @arg TIM_Channel_2: TIM Channel 2
  *            @arg TIM_Channel_3: TIM Channel 3
  *            @arg TIM_Channel_4: TIM Channel 4
  * @param  TIM_CCx: specifies the TIM Channel CCxE bit new state.
  *          This parameter can be: TIM_CCx_Enable or TIM_CCx_Disable. 
  * @retval None
  */
void TIM_CCxCmd(TIM_TypeDef* TIMx, uint16_t TIM_Channel, uint16_t TIM_CCx)
{
  uint16_t tmp = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx)); 
  assert_param(IS_TIM_CHANNEL(TIM_Channel));
  assert_param(IS_TIM_CCX(TIM_CCx));

  tmp = CCER_CCE_SET << TIM_Channel;

  /* Reset the CCxE Bit */
  TIMx->CCER &= (uint16_t)~ tmp;

  /* Set or reset the CCxE Bit */ 
  TIMx->CCER |=  (uint16_t)(TIM_CCx << TIM_Channel);
}

/**
  * @brief  Enables or disables the TIM Capture Compare Channel xN.
  * @param  TIMx: where x can be 1 or 8 to select the TIM peripheral.
  * @param  TIM_Channel: specifies the TIM Channel
  *          This parameter can be one of the following values:
  *            @arg TIM_Channel_1: TIM Channel 1
  *            @arg TIM_Channel_2: TIM Channel 2
  *            @arg TIM_Channel_3: TIM Channel 3
  * @param  TIM_CCxN: specifies the TIM Channel CCxNE bit new state.
  *          This parameter can be: TIM_CCxN_Enable or TIM_CCxN_Disable. 
  * @retval None
  */
void TIM_CCxNCmd(TIM_TypeDef* TIMx, uint16_t TIM_Channel, uint16_t TIM_CCxN)
{
  uint16_t tmp = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST4_PERIPH(TIMx));
  assert_param(IS_TIM_COMPLEMENTARY_CHANNEL(TIM_Channel));
  assert_param(IS_TIM_CCXN(TIM_CCxN));

  tmp = CCER_CCNE_SET << TIM_Channel;

  /* Reset the CCxNE Bit */
  TIMx->CCER &= (uint16_t) ~tmp;

  /* Set or reset the CCxNE Bit */ 
  TIMx->CCER |=  (uint16_t)(TIM_CCxN << TIM_Channel);
}
/**
  * @}
  */

/** @defgroup TIM_Group3 Input Capture management functions
 *  @brief    Input Capture management functions 
 *
@verbatim   
 ===============================================================================
                      Input Capture management functions
 ===============================================================================  
   
       ===================================================================      
              TIM Driver: how to use it in Input Capture Mode
       =================================================================== 
       To use the Timer in Input Capture mode, the following steps are mandatory:
       
       1. Enable TIM clock using RCC_APBxPeriphClockCmd(RCC_APBxPeriph_TIMx, ENABLE) function
       
       2. Configure the TIM pins by configuring the corresponding GPIO pins
       
       2. Configure the Time base unit as described in the first part of this driver,
          if needed, else the Timer will run with the default configuration:
          - Autoreload value = 0xFFFF
          - Prescaler value = 0x0000
          - Counter mode = Up counting
          - Clock Division = TIM_CKD_DIV1
          
       3. Fill the TIM_ICInitStruct with the desired parameters including:
          - TIM Channel: TIM_Channel
          - TIM Input Capture polarity: TIM_ICPolarity
          - TIM Input Capture selection: TIM_ICSelection
          - TIM Input Capture Prescaler: TIM_ICPrescaler
          - TIM Input CApture filter value: TIM_ICFilter
       
       4. Call TIM_ICInit(TIMx, &TIM_ICInitStruct) to configure the desired channel with the 
          corresponding configuration and to measure only frequency or duty cycle of the input signal,
          or,
          Call TIM_PWMIConfig(TIMx, &TIM_ICInitStruct) to configure the desired channels with the 
          corresponding configuration and to measure the frequency and the duty cycle of the input signal
          
       5. Enable the NVIC or the DMA to read the measured frequency. 
          
       6. Enable the corresponding interrupt (or DMA request) to read the Captured value,
          using the function TIM_ITConfig(TIMx, TIM_IT_CCx) (or TIM_DMA_Cmd(TIMx, TIM_DMA_CCx)) 
       
       7. Call the TIM_Cmd(ENABLE) function to enable the TIM counter.
       
       8. Use TIM_GetCapturex(TIMx); to read the captured value.
       
       Note1: All other functions can be used separately to modify, if needed,
              a specific feature of the Timer. 

@endverbatim
  * @{
  */

/**
  * @brief  Initializes the TIM peripheral according to the specified parameters
  *         in the TIM_ICInitStruct.
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  TIM_ICInitStruct: pointer to a TIM_ICInitTypeDef structure that contains
  *         the configuration information for the specified TIM peripheral.
  * @retval None
  */
void TIM_ICInit(TIM_TypeDef* TIMx, TIM_ICInitTypeDef* TIM_ICInitStruct)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));
  assert_param(IS_TIM_IC_POLARITY(TIM_ICInitStruct->TIM_ICPolarity));
  assert_param(IS_TIM_IC_SELECTION(TIM_ICInitStruct->TIM_ICSelection));
  assert_param(IS_TIM_IC_PRESCALER(TIM_ICInitStruct->TIM_ICPrescaler));
  assert_param(IS_TIM_IC_FILTER(TIM_ICInitStruct->TIM_ICFilter));
  
  if (TIM_ICInitStruct->TIM_Channel == TIM_Channel_1)
  {
    /* TI1 Configuration */
    TI1_Config(TIMx, TIM_ICInitStruct->TIM_ICPolarity,
               TIM_ICInitStruct->TIM_ICSelection,
               TIM_ICInitStruct->TIM_ICFilter);
    /* Set the Input Capture Prescaler value */
    TIM_SetIC1Prescaler(TIMx, TIM_ICInitStruct->TIM_ICPrescaler);
  }
  else if (TIM_ICInitStruct->TIM_Channel == TIM_Channel_2)
  {
    /* TI2 Configuration */
    assert_param(IS_TIM_LIST2_PERIPH(TIMx));
    TI2_Config(TIMx, TIM_ICInitStruct->TIM_ICPolarity,
               TIM_ICInitStruct->TIM_ICSelection,
               TIM_ICInitStruct->TIM_ICFilter);
    /* Set the Input Capture Prescaler value */
    TIM_SetIC2Prescaler(TIMx, TIM_ICInitStruct->TIM_ICPrescaler);
  }
  else if (TIM_ICInitStruct->TIM_Channel == TIM_Channel_3)
  {
    /* TI3 Configuration */
    assert_param(IS_TIM_LIST3_PERIPH(TIMx));
    TI3_Config(TIMx,  TIM_ICInitStruct->TIM_ICPolarity,
               TIM_ICInitStruct->TIM_ICSelection,
               TIM_ICInitStruct->TIM_ICFilter);
    /* Set the Input Capture Prescaler value */
    TIM_SetIC3Prescaler(TIMx, TIM_ICInitStruct->TIM_ICPrescaler);
  }
  else
  {
    /* TI4 Configuration */
    assert_param(IS_TIM_LIST3_PERIPH(TIMx));
    TI4_Config(TIMx, TIM_ICInitStruct->TIM_ICPolarity,
               TIM_ICInitStruct->TIM_ICSelection,
               TIM_ICInitStruct->TIM_ICFilter);
    /* Set the Input Capture Prescaler value */
    TIM_SetIC4Prescaler(TIMx, TIM_ICInitStruct->TIM_ICPrescaler);
  }
}

/**
  * @brief  Fills each TIM_ICInitStruct member with its default value.
  * @param  TIM_ICInitStruct: pointer to a TIM_ICInitTypeDef structure which will
  *         be initialized.
  * @retval None
  */
void TIM_ICStructInit(TIM_ICInitTypeDef* TIM_ICInitStruct)
{
  /* Set the default configuration */
  TIM_ICInitStruct->TIM_Channel = TIM_Channel_1;
  TIM_ICInitStruct->TIM_ICPolarity = TIM_ICPolarity_Rising;
  TIM_ICInitStruct->TIM_ICSelection = TIM_ICSelection_DirectTI;
  TIM_ICInitStruct->TIM_ICPrescaler = TIM_ICPSC_DIV1;
  TIM_ICInitStruct->TIM_ICFilter = 0x00;
}

/**
  * @brief  Configures the TIM peripheral according to the specified parameters
  *         in the TIM_ICInitStruct to measure an external PWM signal.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5,8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  TIM_ICInitStruct: pointer to a TIM_ICInitTypeDef structure that contains
  *         the configuration information for the specified TIM peripheral.
  * @retval None
  */
void TIM_PWMIConfig(TIM_TypeDef* TIMx, TIM_ICInitTypeDef* TIM_ICInitStruct)
{
  uint16_t icoppositepolarity = TIM_ICPolarity_Rising;
  uint16_t icoppositeselection = TIM_ICSelection_DirectTI;

  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));

  /* Select the Opposite Input Polarity */
  if (TIM_ICInitStruct->TIM_ICPolarity == TIM_ICPolarity_Rising)
  {
    icoppositepolarity = TIM_ICPolarity_Falling;
  }
  else
  {
    icoppositepolarity = TIM_ICPolarity_Rising;
  }
  /* Select the Opposite Input */
  if (TIM_ICInitStruct->TIM_ICSelection == TIM_ICSelection_DirectTI)
  {
    icoppositeselection = TIM_ICSelection_IndirectTI;
  }
  else
  {
    icoppositeselection = TIM_ICSelection_DirectTI;
  }
  if (TIM_ICInitStruct->TIM_Channel == TIM_Channel_1)
  {
    /* TI1 Configuration */
    TI1_Config(TIMx, TIM_ICInitStruct->TIM_ICPolarity, TIM_ICInitStruct->TIM_ICSelection,
               TIM_ICInitStruct->TIM_ICFilter);
    /* Set the Input Capture Prescaler value */
    TIM_SetIC1Prescaler(TIMx, TIM_ICInitStruct->TIM_ICPrescaler);
    /* TI2 Configuration */
    TI2_Config(TIMx, icoppositepolarity, icoppositeselection, TIM_ICInitStruct->TIM_ICFilter);
    /* Set the Input Capture Prescaler value */
    TIM_SetIC2Prescaler(TIMx, TIM_ICInitStruct->TIM_ICPrescaler);
  }
  else
  { 
    /* TI2 Configuration */
    TI2_Config(TIMx, TIM_ICInitStruct->TIM_ICPolarity, TIM_ICInitStruct->TIM_ICSelection,
               TIM_ICInitStruct->TIM_ICFilter);
    /* Set the Input Capture Prescaler value */
    TIM_SetIC2Prescaler(TIMx, TIM_ICInitStruct->TIM_ICPrescaler);
    /* TI1 Configuration */
    TI1_Config(TIMx, icoppositepolarity, icoppositeselection, TIM_ICInitStruct->TIM_ICFilter);
    /* Set the Input Capture Prescaler value */
    TIM_SetIC1Prescaler(TIMx, TIM_ICInitStruct->TIM_ICPrescaler);
  }
}

/**
  * @brief  Gets the TIMx Input Capture 1 value.
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @retval Capture Compare 1 Register value.
  */
uint32_t TIM_GetCapture1(TIM_TypeDef* TIMx)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));

  /* Get the Capture 1 Register value */
  return TIMx->CCR1;
}

/**
  * @brief  Gets the TIMx Input Capture 2 value.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @retval Capture Compare 2 Register value.
  */
uint32_t TIM_GetCapture2(TIM_TypeDef* TIMx)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));

  /* Get the Capture 2 Register value */
  return TIMx->CCR2;
}

/**
  * @brief  Gets the TIMx Input Capture 3 value.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @retval Capture Compare 3 Register value.
  */
uint32_t TIM_GetCapture3(TIM_TypeDef* TIMx)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx)); 

  /* Get the Capture 3 Register value */
  return TIMx->CCR3;
}

/**
  * @brief  Gets the TIMx Input Capture 4 value.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @retval Capture Compare 4 Register value.
  */
uint32_t TIM_GetCapture4(TIM_TypeDef* TIMx)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));

  /* Get the Capture 4 Register value */
  return TIMx->CCR4;
}

/**
  * @brief  Sets the TIMx Input Capture 1 prescaler.
  * @param  TIMx: where x can be 1 to 14 except 6 and 7, to select the TIM peripheral.
  * @param  TIM_ICPSC: specifies the Input Capture1 prescaler new value.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICPSC_DIV1: no prescaler
  *            @arg TIM_ICPSC_DIV2: capture is done once every 2 events
  *            @arg TIM_ICPSC_DIV4: capture is done once every 4 events
  *            @arg TIM_ICPSC_DIV8: capture is done once every 8 events
  * @retval None
  */
void TIM_SetIC1Prescaler(TIM_TypeDef* TIMx, uint16_t TIM_ICPSC)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));
  assert_param(IS_TIM_IC_PRESCALER(TIM_ICPSC));

  /* Reset the IC1PSC Bits */
  TIMx->CCMR1 &= (uint16_t)~TIM_CCMR1_IC1PSC;

  /* Set the IC1PSC value */
  TIMx->CCMR1 |= TIM_ICPSC;
}

/**
  * @brief  Sets the TIMx Input Capture 2 prescaler.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  TIM_ICPSC: specifies the Input Capture2 prescaler new value.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICPSC_DIV1: no prescaler
  *            @arg TIM_ICPSC_DIV2: capture is done once every 2 events
  *            @arg TIM_ICPSC_DIV4: capture is done once every 4 events
  *            @arg TIM_ICPSC_DIV8: capture is done once every 8 events
  * @retval None
  */
void TIM_SetIC2Prescaler(TIM_TypeDef* TIMx, uint16_t TIM_ICPSC)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));
  assert_param(IS_TIM_IC_PRESCALER(TIM_ICPSC));

  /* Reset the IC2PSC Bits */
  TIMx->CCMR1 &= (uint16_t)~TIM_CCMR1_IC2PSC;

  /* Set the IC2PSC value */
  TIMx->CCMR1 |= (uint16_t)(TIM_ICPSC << 8);
}

/**
  * @brief  Sets the TIMx Input Capture 3 prescaler.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_ICPSC: specifies the Input Capture3 prescaler new value.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICPSC_DIV1: no prescaler
  *            @arg TIM_ICPSC_DIV2: capture is done once every 2 events
  *            @arg TIM_ICPSC_DIV4: capture is done once every 4 events
  *            @arg TIM_ICPSC_DIV8: capture is done once every 8 events
  * @retval None
  */
void TIM_SetIC3Prescaler(TIM_TypeDef* TIMx, uint16_t TIM_ICPSC)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_IC_PRESCALER(TIM_ICPSC));

  /* Reset the IC3PSC Bits */
  TIMx->CCMR2 &= (uint16_t)~TIM_CCMR2_IC3PSC;

  /* Set the IC3PSC value */
  TIMx->CCMR2 |= TIM_ICPSC;
}

/**
  * @brief  Sets the TIMx Input Capture 4 prescaler.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_ICPSC: specifies the Input Capture4 prescaler new value.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICPSC_DIV1: no prescaler
  *            @arg TIM_ICPSC_DIV2: capture is done once every 2 events
  *            @arg TIM_ICPSC_DIV4: capture is done once every 4 events
  *            @arg TIM_ICPSC_DIV8: capture is done once every 8 events
  * @retval None
  */
void TIM_SetIC4Prescaler(TIM_TypeDef* TIMx, uint16_t TIM_ICPSC)
{  
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_IC_PRESCALER(TIM_ICPSC));

  /* Reset the IC4PSC Bits */
  TIMx->CCMR2 &= (uint16_t)~TIM_CCMR2_IC4PSC;

  /* Set the IC4PSC value */
  TIMx->CCMR2 |= (uint16_t)(TIM_ICPSC << 8);
}
/**
  * @}
  */

/** @defgroup TIM_Group4 Advanced-control timers (TIM1 and TIM8) specific features
 *  @brief   Advanced-control timers (TIM1 and TIM8) specific features
 *
@verbatim   
 ===============================================================================
          Advanced-control timers (TIM1 and TIM8) specific features
 ===============================================================================  
  
       ===================================================================      
              TIM Driver: how to use the Break feature
       =================================================================== 
       After configuring the Timer channel(s) in the appropriate Output Compare mode: 
                         
       1. Fill the TIM_BDTRInitStruct with the desired parameters for the Timer
          Break Polarity, dead time, Lock level, the OSSI/OSSR State and the 
          AOE(automatic output enable).
               
       2. Call TIM_BDTRConfig(TIMx, &TIM_BDTRInitStruct) to configure the Timer
          
       3. Enable the Main Output using TIM_CtrlPWMOutputs(TIM1, ENABLE) 
          
       4. Once the break even occurs, the Timer's output signals are put in reset
          state or in a known state (according to the configuration made in
          TIM_BDTRConfig() function).

@endverbatim
  * @{
  */

/**
  * @brief  Configures the Break feature, dead time, Lock level, OSSI/OSSR State
  *         and the AOE(automatic output enable).
  * @param  TIMx: where x can be  1 or 8 to select the TIM 
  * @param  TIM_BDTRInitStruct: pointer to a TIM_BDTRInitTypeDef structure that
  *         contains the BDTR Register configuration  information for the TIM peripheral.
  * @retval None
  */
void TIM_BDTRConfig(TIM_TypeDef* TIMx, TIM_BDTRInitTypeDef *TIM_BDTRInitStruct)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST4_PERIPH(TIMx));
  assert_param(IS_TIM_OSSR_STATE(TIM_BDTRInitStruct->TIM_OSSRState));
  assert_param(IS_TIM_OSSI_STATE(TIM_BDTRInitStruct->TIM_OSSIState));
  assert_param(IS_TIM_LOCK_LEVEL(TIM_BDTRInitStruct->TIM_LOCKLevel));
  assert_param(IS_TIM_BREAK_STATE(TIM_BDTRInitStruct->TIM_Break));
  assert_param(IS_TIM_BREAK_POLARITY(TIM_BDTRInitStruct->TIM_BreakPolarity));
  assert_param(IS_TIM_AUTOMATIC_OUTPUT_STATE(TIM_BDTRInitStruct->TIM_AutomaticOutput));

  /* Set the Lock level, the Break enable Bit and the Polarity, the OSSR State,
     the OSSI State, the dead time value and the Automatic Output Enable Bit */
  TIMx->BDTR = (uint32_t)TIM_BDTRInitStruct->TIM_OSSRState | TIM_BDTRInitStruct->TIM_OSSIState |
             TIM_BDTRInitStruct->TIM_LOCKLevel | TIM_BDTRInitStruct->TIM_DeadTime |
             TIM_BDTRInitStruct->TIM_Break | TIM_BDTRInitStruct->TIM_BreakPolarity |
             TIM_BDTRInitStruct->TIM_AutomaticOutput;
}

/**
  * @brief  Fills each TIM_BDTRInitStruct member with its default value.
  * @param  TIM_BDTRInitStruct: pointer to a TIM_BDTRInitTypeDef structure which
  *         will be initialized.
  * @retval None
  */
void TIM_BDTRStructInit(TIM_BDTRInitTypeDef* TIM_BDTRInitStruct)
{
  /* Set the default configuration */
  TIM_BDTRInitStruct->TIM_OSSRState = TIM_OSSRState_Disable;
  TIM_BDTRInitStruct->TIM_OSSIState = TIM_OSSIState_Disable;
  TIM_BDTRInitStruct->TIM_LOCKLevel = TIM_LOCKLevel_OFF;
  TIM_BDTRInitStruct->TIM_DeadTime = 0x00;
  TIM_BDTRInitStruct->TIM_Break = TIM_Break_Disable;
  TIM_BDTRInitStruct->TIM_BreakPolarity = TIM_BreakPolarity_Low;
  TIM_BDTRInitStruct->TIM_AutomaticOutput = TIM_AutomaticOutput_Disable;
}

/**
  * @brief  Enables or disables the TIM peripheral Main Outputs.
  * @param  TIMx: where x can be 1 or 8 to select the TIMx peripheral.
  * @param  NewState: new state of the TIM peripheral Main Outputs.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void TIM_CtrlPWMOutputs(TIM_TypeDef* TIMx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST4_PERIPH(TIMx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Enable the TIM Main Output */
    TIMx->BDTR |= TIM_BDTR_MOE;
  }
  else
  {
    /* Disable the TIM Main Output */
    TIMx->BDTR &= (uint16_t)~TIM_BDTR_MOE;
  }  
}

/**
  * @brief  Selects the TIM peripheral Commutation event.
  * @param  TIMx: where x can be  1 or 8 to select the TIMx peripheral
  * @param  NewState: new state of the Commutation event.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void TIM_SelectCOM(TIM_TypeDef* TIMx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST4_PERIPH(TIMx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Set the COM Bit */
    TIMx->CR2 |= TIM_CR2_CCUS;
  }
  else
  {
    /* Reset the COM Bit */
    TIMx->CR2 &= (uint16_t)~TIM_CR2_CCUS;
  }
}

/**
  * @brief  Sets or Resets the TIM peripheral Capture Compare Preload Control bit.
  * @param  TIMx: where x can be  1 or 8 to select the TIMx peripheral
  * @param  NewState: new state of the Capture Compare Preload Control bit
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void TIM_CCPreloadControl(TIM_TypeDef* TIMx, FunctionalState NewState)
{ 
  /* Check the parameters */
  assert_param(IS_TIM_LIST4_PERIPH(TIMx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Set the CCPC Bit */
    TIMx->CR2 |= TIM_CR2_CCPC;
  }
  else
  {
    /* Reset the CCPC Bit */
    TIMx->CR2 &= (uint16_t)~TIM_CR2_CCPC;
  }
}
/**
  * @}
  */

/** @defgroup TIM_Group5 Interrupts DMA and flags management functions
 *  @brief    Interrupts, DMA and flags management functions 
 *
@verbatim   
 ===============================================================================
                 Interrupts, DMA and flags management functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the specified TIM interrupts.
  * @param  TIMx: where x can be 1 to 14 to select the TIMx peripheral.
  * @param  TIM_IT: specifies the TIM interrupts sources to be enabled or disabled.
  *          This parameter can be any combination of the following values:
  *            @arg TIM_IT_Update: TIM update Interrupt source
  *            @arg TIM_IT_CC1: TIM Capture Compare 1 Interrupt source
  *            @arg TIM_IT_CC2: TIM Capture Compare 2 Interrupt source
  *            @arg TIM_IT_CC3: TIM Capture Compare 3 Interrupt source
  *            @arg TIM_IT_CC4: TIM Capture Compare 4 Interrupt source
  *            @arg TIM_IT_COM: TIM Commutation Interrupt source
  *            @arg TIM_IT_Trigger: TIM Trigger Interrupt source
  *            @arg TIM_IT_Break: TIM Break Interrupt source
  *  
  * @note   For TIM6 and TIM7 only the parameter TIM_IT_Update can be used
  * @note   For TIM9 and TIM12 only one of the following parameters can be used: TIM_IT_Update,
  *          TIM_IT_CC1, TIM_IT_CC2 or TIM_IT_Trigger. 
  * @note   For TIM10, TIM11, TIM13 and TIM14 only one of the following parameters can
  *          be used: TIM_IT_Update or TIM_IT_CC1   
  * @note   TIM_IT_COM and TIM_IT_Break can be used only with TIM1 and TIM8 
  *        
  * @param  NewState: new state of the TIM interrupts.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void TIM_ITConfig(TIM_TypeDef* TIMx, uint16_t TIM_IT, FunctionalState NewState)
{  
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));
  assert_param(IS_TIM_IT(TIM_IT));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the Interrupt sources */
    TIMx->DIER |= TIM_IT;
  }
  else
  {
    /* Disable the Interrupt sources */
    TIMx->DIER &= (uint16_t)~TIM_IT;
  }
}

/**
  * @brief  Configures the TIMx event to be generate by software.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @param  TIM_EventSource: specifies the event source.
  *          This parameter can be one or more of the following values:	   
  *            @arg TIM_EventSource_Update: Timer update Event source
  *            @arg TIM_EventSource_CC1: Timer Capture Compare 1 Event source
  *            @arg TIM_EventSource_CC2: Timer Capture Compare 2 Event source
  *            @arg TIM_EventSource_CC3: Timer Capture Compare 3 Event source
  *            @arg TIM_EventSource_CC4: Timer Capture Compare 4 Event source
  *            @arg TIM_EventSource_COM: Timer COM event source  
  *            @arg TIM_EventSource_Trigger: Timer Trigger Event source
  *            @arg TIM_EventSource_Break: Timer Break event source
  * 
  * @note   TIM6 and TIM7 can only generate an update event. 
  * @note   TIM_EventSource_COM and TIM_EventSource_Break are used only with TIM1 and TIM8.
  *        
  * @retval None
  */
void TIM_GenerateEvent(TIM_TypeDef* TIMx, uint16_t TIM_EventSource)
{ 
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));
  assert_param(IS_TIM_EVENT_SOURCE(TIM_EventSource));
 
  /* Set the event sources */
  TIMx->EGR = TIM_EventSource;
}

/**
  * @brief  Checks whether the specified TIM flag is set or not.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @param  TIM_FLAG: specifies the flag to check.
  *          This parameter can be one of the following values:
  *            @arg TIM_FLAG_Update: TIM update Flag
  *            @arg TIM_FLAG_CC1: TIM Capture Compare 1 Flag
  *            @arg TIM_FLAG_CC2: TIM Capture Compare 2 Flag
  *            @arg TIM_FLAG_CC3: TIM Capture Compare 3 Flag
  *            @arg TIM_FLAG_CC4: TIM Capture Compare 4 Flag
  *            @arg TIM_FLAG_COM: TIM Commutation Flag
  *            @arg TIM_FLAG_Trigger: TIM Trigger Flag
  *            @arg TIM_FLAG_Break: TIM Break Flag
  *            @arg TIM_FLAG_CC1OF: TIM Capture Compare 1 over capture Flag
  *            @arg TIM_FLAG_CC2OF: TIM Capture Compare 2 over capture Flag
  *            @arg TIM_FLAG_CC3OF: TIM Capture Compare 3 over capture Flag
  *            @arg TIM_FLAG_CC4OF: TIM Capture Compare 4 over capture Flag
  *
  * @note   TIM6 and TIM7 can have only one update flag. 
  * @note   TIM_FLAG_COM and TIM_FLAG_Break are used only with TIM1 and TIM8.    
  *
  * @retval The new state of TIM_FLAG (SET or RESET).
  */
FlagStatus TIM_GetFlagStatus(TIM_TypeDef* TIMx, uint16_t TIM_FLAG)
{ 
  ITStatus bitstatus = RESET;  
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));
  assert_param(IS_TIM_GET_FLAG(TIM_FLAG));

  
  if ((TIMx->SR & TIM_FLAG) != (uint16_t)RESET)
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
  * @brief  Clears the TIMx's pending flags.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @param  TIM_FLAG: specifies the flag bit to clear.
  *          This parameter can be any combination of the following values:
  *            @arg TIM_FLAG_Update: TIM update Flag
  *            @arg TIM_FLAG_CC1: TIM Capture Compare 1 Flag
  *            @arg TIM_FLAG_CC2: TIM Capture Compare 2 Flag
  *            @arg TIM_FLAG_CC3: TIM Capture Compare 3 Flag
  *            @arg TIM_FLAG_CC4: TIM Capture Compare 4 Flag
  *            @arg TIM_FLAG_COM: TIM Commutation Flag
  *            @arg TIM_FLAG_Trigger: TIM Trigger Flag
  *            @arg TIM_FLAG_Break: TIM Break Flag
  *            @arg TIM_FLAG_CC1OF: TIM Capture Compare 1 over capture Flag
  *            @arg TIM_FLAG_CC2OF: TIM Capture Compare 2 over capture Flag
  *            @arg TIM_FLAG_CC3OF: TIM Capture Compare 3 over capture Flag
  *            @arg TIM_FLAG_CC4OF: TIM Capture Compare 4 over capture Flag
  *
  * @note   TIM6 and TIM7 can have only one update flag. 
  * @note   TIM_FLAG_COM and TIM_FLAG_Break are used only with TIM1 and TIM8.
  *    
  * @retval None
  */
void TIM_ClearFlag(TIM_TypeDef* TIMx, uint16_t TIM_FLAG)
{  
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));
   
  /* Clear the flags */
  TIMx->SR = (uint16_t)~TIM_FLAG;
}

/**
  * @brief  Checks whether the TIM interrupt has occurred or not.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @param  TIM_IT: specifies the TIM interrupt source to check.
  *          This parameter can be one of the following values:
  *            @arg TIM_IT_Update: TIM update Interrupt source
  *            @arg TIM_IT_CC1: TIM Capture Compare 1 Interrupt source
  *            @arg TIM_IT_CC2: TIM Capture Compare 2 Interrupt source
  *            @arg TIM_IT_CC3: TIM Capture Compare 3 Interrupt source
  *            @arg TIM_IT_CC4: TIM Capture Compare 4 Interrupt source
  *            @arg TIM_IT_COM: TIM Commutation Interrupt source
  *            @arg TIM_IT_Trigger: TIM Trigger Interrupt source
  *            @arg TIM_IT_Break: TIM Break Interrupt source
  *
  * @note   TIM6 and TIM7 can generate only an update interrupt.
  * @note   TIM_IT_COM and TIM_IT_Break are used only with TIM1 and TIM8.
  *     
  * @retval The new state of the TIM_IT(SET or RESET).
  */
ITStatus TIM_GetITStatus(TIM_TypeDef* TIMx, uint16_t TIM_IT)
{
  ITStatus bitstatus = RESET;  
  uint16_t itstatus = 0x0, itenable = 0x0;
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));
  assert_param(IS_TIM_GET_IT(TIM_IT));
   
  itstatus = TIMx->SR & TIM_IT;
  
  itenable = TIMx->DIER & TIM_IT;
  if ((itstatus != (uint16_t)RESET) && (itenable != (uint16_t)RESET))
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
  * @brief  Clears the TIMx's interrupt pending bits.
  * @param  TIMx: where x can be 1 to 14 to select the TIM peripheral.
  * @param  TIM_IT: specifies the pending bit to clear.
  *          This parameter can be any combination of the following values:
  *            @arg TIM_IT_Update: TIM1 update Interrupt source
  *            @arg TIM_IT_CC1: TIM Capture Compare 1 Interrupt source
  *            @arg TIM_IT_CC2: TIM Capture Compare 2 Interrupt source
  *            @arg TIM_IT_CC3: TIM Capture Compare 3 Interrupt source
  *            @arg TIM_IT_CC4: TIM Capture Compare 4 Interrupt source
  *            @arg TIM_IT_COM: TIM Commutation Interrupt source
  *            @arg TIM_IT_Trigger: TIM Trigger Interrupt source
  *            @arg TIM_IT_Break: TIM Break Interrupt source
  *
  * @note   TIM6 and TIM7 can generate only an update interrupt.
  * @note   TIM_IT_COM and TIM_IT_Break are used only with TIM1 and TIM8.
  *      
  * @retval None
  */
void TIM_ClearITPendingBit(TIM_TypeDef* TIMx, uint16_t TIM_IT)
{
  /* Check the parameters */
  assert_param(IS_TIM_ALL_PERIPH(TIMx));

  /* Clear the IT pending Bit */
  TIMx->SR = (uint16_t)~TIM_IT;
}

/**
  * @brief  Configures the TIMx's DMA interface.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_DMABase: DMA Base address.
  *          This parameter can be one of the following values:
  *            @arg TIM_DMABase_CR1  
  *            @arg TIM_DMABase_CR2
  *            @arg TIM_DMABase_SMCR
  *            @arg TIM_DMABase_DIER
  *            @arg TIM1_DMABase_SR
  *            @arg TIM_DMABase_EGR
  *            @arg TIM_DMABase_CCMR1
  *            @arg TIM_DMABase_CCMR2
  *            @arg TIM_DMABase_CCER
  *            @arg TIM_DMABase_CNT   
  *            @arg TIM_DMABase_PSC   
  *            @arg TIM_DMABase_ARR
  *            @arg TIM_DMABase_RCR
  *            @arg TIM_DMABase_CCR1
  *            @arg TIM_DMABase_CCR2
  *            @arg TIM_DMABase_CCR3  
  *            @arg TIM_DMABase_CCR4
  *            @arg TIM_DMABase_BDTR
  *            @arg TIM_DMABase_DCR
  * @param  TIM_DMABurstLength: DMA Burst length. This parameter can be one value
  *         between: TIM_DMABurstLength_1Transfer and TIM_DMABurstLength_18Transfers.
  * @retval None
  */
void TIM_DMAConfig(TIM_TypeDef* TIMx, uint16_t TIM_DMABase, uint16_t TIM_DMABurstLength)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_DMA_BASE(TIM_DMABase)); 
  assert_param(IS_TIM_DMA_LENGTH(TIM_DMABurstLength));

  /* Set the DMA Base and the DMA Burst Length */
  TIMx->DCR = TIM_DMABase | TIM_DMABurstLength;
}

/**
  * @brief  Enables or disables the TIMx's DMA Requests.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 6, 7 or 8 to select the TIM peripheral.
  * @param  TIM_DMASource: specifies the DMA Request sources.
  *          This parameter can be any combination of the following values:
  *            @arg TIM_DMA_Update: TIM update Interrupt source
  *            @arg TIM_DMA_CC1: TIM Capture Compare 1 DMA source
  *            @arg TIM_DMA_CC2: TIM Capture Compare 2 DMA source
  *            @arg TIM_DMA_CC3: TIM Capture Compare 3 DMA source
  *            @arg TIM_DMA_CC4: TIM Capture Compare 4 DMA source
  *            @arg TIM_DMA_COM: TIM Commutation DMA source
  *            @arg TIM_DMA_Trigger: TIM Trigger DMA source
  * @param  NewState: new state of the DMA Request sources.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void TIM_DMACmd(TIM_TypeDef* TIMx, uint16_t TIM_DMASource, FunctionalState NewState)
{ 
  /* Check the parameters */
  assert_param(IS_TIM_LIST5_PERIPH(TIMx)); 
  assert_param(IS_TIM_DMA_SOURCE(TIM_DMASource));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the DMA sources */
    TIMx->DIER |= TIM_DMASource; 
  }
  else
  {
    /* Disable the DMA sources */
    TIMx->DIER &= (uint16_t)~TIM_DMASource;
  }
}

/**
  * @brief  Selects the TIMx peripheral Capture Compare DMA source.
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  NewState: new state of the Capture Compare DMA source
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void TIM_SelectCCDMA(TIM_TypeDef* TIMx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Set the CCDS Bit */
    TIMx->CR2 |= TIM_CR2_CCDS;
  }
  else
  {
    /* Reset the CCDS Bit */
    TIMx->CR2 &= (uint16_t)~TIM_CR2_CCDS;
  }
}
/**
  * @}
  */

/** @defgroup TIM_Group6 Clocks management functions
 *  @brief    Clocks management functions
 *
@verbatim   
 ===============================================================================
                         Clocks management functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Configures the TIMx internal Clock
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @retval None
  */
void TIM_InternalClockConfig(TIM_TypeDef* TIMx)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));

  /* Disable slave mode to clock the prescaler directly with the internal clock */
  TIMx->SMCR &=  (uint16_t)~TIM_SMCR_SMS;
}

/**
  * @brief  Configures the TIMx Internal Trigger as External Clock
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  TIM_InputTriggerSource: Trigger source.
  *          This parameter can be one of the following values:
  *            @arg TIM_TS_ITR0: Internal Trigger 0
  *            @arg TIM_TS_ITR1: Internal Trigger 1
  *            @arg TIM_TS_ITR2: Internal Trigger 2
  *            @arg TIM_TS_ITR3: Internal Trigger 3
  * @retval None
  */
void TIM_ITRxExternalClockConfig(TIM_TypeDef* TIMx, uint16_t TIM_InputTriggerSource)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));
  assert_param(IS_TIM_INTERNAL_TRIGGER_SELECTION(TIM_InputTriggerSource));

  /* Select the Internal Trigger */
  TIM_SelectInputTrigger(TIMx, TIM_InputTriggerSource);

  /* Select the External clock mode1 */
  TIMx->SMCR |= TIM_SlaveMode_External1;
}

/**
  * @brief  Configures the TIMx Trigger as External Clock
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13 or 14  
  *         to select the TIM peripheral.
  * @param  TIM_TIxExternalCLKSource: Trigger source.
  *          This parameter can be one of the following values:
  *            @arg TIM_TIxExternalCLK1Source_TI1ED: TI1 Edge Detector
  *            @arg TIM_TIxExternalCLK1Source_TI1: Filtered Timer Input 1
  *            @arg TIM_TIxExternalCLK1Source_TI2: Filtered Timer Input 2
  * @param  TIM_ICPolarity: specifies the TIx Polarity.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICPolarity_Rising
  *            @arg TIM_ICPolarity_Falling
  * @param  ICFilter: specifies the filter value.
  *          This parameter must be a value between 0x0 and 0xF.
  * @retval None
  */
void TIM_TIxExternalClockConfig(TIM_TypeDef* TIMx, uint16_t TIM_TIxExternalCLKSource,
                                uint16_t TIM_ICPolarity, uint16_t ICFilter)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx));
  assert_param(IS_TIM_IC_POLARITY(TIM_ICPolarity));
  assert_param(IS_TIM_IC_FILTER(ICFilter));

  /* Configure the Timer Input Clock Source */
  if (TIM_TIxExternalCLKSource == TIM_TIxExternalCLK1Source_TI2)
  {
    TI2_Config(TIMx, TIM_ICPolarity, TIM_ICSelection_DirectTI, ICFilter);
  }
  else
  {
    TI1_Config(TIMx, TIM_ICPolarity, TIM_ICSelection_DirectTI, ICFilter);
  }
  /* Select the Trigger source */
  TIM_SelectInputTrigger(TIMx, TIM_TIxExternalCLKSource);
  /* Select the External clock mode1 */
  TIMx->SMCR |= TIM_SlaveMode_External1;
}

/**
  * @brief  Configures the External clock Mode1
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_ExtTRGPrescaler: The external Trigger Prescaler.
  *          This parameter can be one of the following values:
  *            @arg TIM_ExtTRGPSC_OFF: ETRP Prescaler OFF.
  *            @arg TIM_ExtTRGPSC_DIV2: ETRP frequency divided by 2.
  *            @arg TIM_ExtTRGPSC_DIV4: ETRP frequency divided by 4.
  *            @arg TIM_ExtTRGPSC_DIV8: ETRP frequency divided by 8.
  * @param  TIM_ExtTRGPolarity: The external Trigger Polarity.
  *          This parameter can be one of the following values:
  *            @arg TIM_ExtTRGPolarity_Inverted: active low or falling edge active.
  *            @arg TIM_ExtTRGPolarity_NonInverted: active high or rising edge active.
  * @param  ExtTRGFilter: External Trigger Filter.
  *          This parameter must be a value between 0x00 and 0x0F
  * @retval None
  */
void TIM_ETRClockMode1Config(TIM_TypeDef* TIMx, uint16_t TIM_ExtTRGPrescaler,
                            uint16_t TIM_ExtTRGPolarity, uint16_t ExtTRGFilter)
{
  uint16_t tmpsmcr = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_EXT_PRESCALER(TIM_ExtTRGPrescaler));
  assert_param(IS_TIM_EXT_POLARITY(TIM_ExtTRGPolarity));
  assert_param(IS_TIM_EXT_FILTER(ExtTRGFilter));
  /* Configure the ETR Clock source */
  TIM_ETRConfig(TIMx, TIM_ExtTRGPrescaler, TIM_ExtTRGPolarity, ExtTRGFilter);
  
  /* Get the TIMx SMCR register value */
  tmpsmcr = TIMx->SMCR;

  /* Reset the SMS Bits */
  tmpsmcr &= (uint16_t)~TIM_SMCR_SMS;

  /* Select the External clock mode1 */
  tmpsmcr |= TIM_SlaveMode_External1;

  /* Select the Trigger selection : ETRF */
  tmpsmcr &= (uint16_t)~TIM_SMCR_TS;
  tmpsmcr |= TIM_TS_ETRF;

  /* Write to TIMx SMCR */
  TIMx->SMCR = tmpsmcr;
}

/**
  * @brief  Configures the External clock Mode2
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_ExtTRGPrescaler: The external Trigger Prescaler.
  *          This parameter can be one of the following values:
  *            @arg TIM_ExtTRGPSC_OFF: ETRP Prescaler OFF.
  *            @arg TIM_ExtTRGPSC_DIV2: ETRP frequency divided by 2.
  *            @arg TIM_ExtTRGPSC_DIV4: ETRP frequency divided by 4.
  *            @arg TIM_ExtTRGPSC_DIV8: ETRP frequency divided by 8.
  * @param  TIM_ExtTRGPolarity: The external Trigger Polarity.
  *          This parameter can be one of the following values:
  *            @arg TIM_ExtTRGPolarity_Inverted: active low or falling edge active.
  *            @arg TIM_ExtTRGPolarity_NonInverted: active high or rising edge active.
  * @param  ExtTRGFilter: External Trigger Filter.
  *          This parameter must be a value between 0x00 and 0x0F
  * @retval None
  */
void TIM_ETRClockMode2Config(TIM_TypeDef* TIMx, uint16_t TIM_ExtTRGPrescaler, 
                             uint16_t TIM_ExtTRGPolarity, uint16_t ExtTRGFilter)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_EXT_PRESCALER(TIM_ExtTRGPrescaler));
  assert_param(IS_TIM_EXT_POLARITY(TIM_ExtTRGPolarity));
  assert_param(IS_TIM_EXT_FILTER(ExtTRGFilter));

  /* Configure the ETR Clock source */
  TIM_ETRConfig(TIMx, TIM_ExtTRGPrescaler, TIM_ExtTRGPolarity, ExtTRGFilter);

  /* Enable the External clock mode2 */
  TIMx->SMCR |= TIM_SMCR_ECE;
}
/**
  * @}
  */

/** @defgroup TIM_Group7 Synchronization management functions
 *  @brief    Synchronization management functions 
 *
@verbatim   
 ===============================================================================
                       Synchronization management functions
 ===============================================================================  
                   
       ===================================================================      
              TIM Driver: how to use it in synchronization Mode
       =================================================================== 
       Case of two/several Timers
       **************************
       1. Configure the Master Timers using the following functions:
          - void TIM_SelectOutputTrigger(TIM_TypeDef* TIMx, uint16_t TIM_TRGOSource); 
          - void TIM_SelectMasterSlaveMode(TIM_TypeDef* TIMx, uint16_t TIM_MasterSlaveMode);  
       2. Configure the Slave Timers using the following functions: 
          - void TIM_SelectInputTrigger(TIM_TypeDef* TIMx, uint16_t TIM_InputTriggerSource);  
          - void TIM_SelectSlaveMode(TIM_TypeDef* TIMx, uint16_t TIM_SlaveMode); 
          
       Case of Timers and external trigger(ETR pin)
       ********************************************       
       1. Configure the External trigger using this function:
          - void TIM_ETRConfig(TIM_TypeDef* TIMx, uint16_t TIM_ExtTRGPrescaler, uint16_t TIM_ExtTRGPolarity,
                               uint16_t ExtTRGFilter);
       2. Configure the Slave Timers using the following functions: 
          - void TIM_SelectInputTrigger(TIM_TypeDef* TIMx, uint16_t TIM_InputTriggerSource);  
          - void TIM_SelectSlaveMode(TIM_TypeDef* TIMx, uint16_t TIM_SlaveMode); 

@endverbatim
  * @{
  */

/**
  * @brief  Selects the Input Trigger source
  * @param  TIMx: where x can be  1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13 or 14  
  *         to select the TIM peripheral.
  * @param  TIM_InputTriggerSource: The Input Trigger source.
  *          This parameter can be one of the following values:
  *            @arg TIM_TS_ITR0: Internal Trigger 0
  *            @arg TIM_TS_ITR1: Internal Trigger 1
  *            @arg TIM_TS_ITR2: Internal Trigger 2
  *            @arg TIM_TS_ITR3: Internal Trigger 3
  *            @arg TIM_TS_TI1F_ED: TI1 Edge Detector
  *            @arg TIM_TS_TI1FP1: Filtered Timer Input 1
  *            @arg TIM_TS_TI2FP2: Filtered Timer Input 2
  *            @arg TIM_TS_ETRF: External Trigger input
  * @retval None
  */
void TIM_SelectInputTrigger(TIM_TypeDef* TIMx, uint16_t TIM_InputTriggerSource)
{
  uint16_t tmpsmcr = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST1_PERIPH(TIMx)); 
  assert_param(IS_TIM_TRIGGER_SELECTION(TIM_InputTriggerSource));

  /* Get the TIMx SMCR register value */
  tmpsmcr = TIMx->SMCR;

  /* Reset the TS Bits */
  tmpsmcr &= (uint16_t)~TIM_SMCR_TS;

  /* Set the Input Trigger source */
  tmpsmcr |= TIM_InputTriggerSource;

  /* Write to TIMx SMCR */
  TIMx->SMCR = tmpsmcr;
}

/**
  * @brief  Selects the TIMx Trigger Output Mode.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 6, 7 or 8 to select the TIM peripheral.
  *     
  * @param  TIM_TRGOSource: specifies the Trigger Output source.
  *   This parameter can be one of the following values:
  *
  *  - For all TIMx
  *            @arg TIM_TRGOSource_Reset:  The UG bit in the TIM_EGR register is used as the trigger output(TRGO)
  *            @arg TIM_TRGOSource_Enable: The Counter Enable CEN is used as the trigger output(TRGO)
  *            @arg TIM_TRGOSource_Update: The update event is selected as the trigger output(TRGO)
  *
  *  - For all TIMx except TIM6 and TIM7
  *            @arg TIM_TRGOSource_OC1: The trigger output sends a positive pulse when the CC1IF flag
  *                                     is to be set, as soon as a capture or compare match occurs(TRGO)
  *            @arg TIM_TRGOSource_OC1Ref: OC1REF signal is used as the trigger output(TRGO)
  *            @arg TIM_TRGOSource_OC2Ref: OC2REF signal is used as the trigger output(TRGO)
  *            @arg TIM_TRGOSource_OC3Ref: OC3REF signal is used as the trigger output(TRGO)
  *            @arg TIM_TRGOSource_OC4Ref: OC4REF signal is used as the trigger output(TRGO)
  *
  * @retval None
  */
void TIM_SelectOutputTrigger(TIM_TypeDef* TIMx, uint16_t TIM_TRGOSource)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST5_PERIPH(TIMx));
  assert_param(IS_TIM_TRGO_SOURCE(TIM_TRGOSource));

  /* Reset the MMS Bits */
  TIMx->CR2 &= (uint16_t)~TIM_CR2_MMS;
  /* Select the TRGO source */
  TIMx->CR2 |=  TIM_TRGOSource;
}

/**
  * @brief  Selects the TIMx Slave Mode.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM peripheral.
  * @param  TIM_SlaveMode: specifies the Timer Slave Mode.
  *          This parameter can be one of the following values:
  *            @arg TIM_SlaveMode_Reset: Rising edge of the selected trigger signal(TRGI) reinitialize 
  *                                      the counter and triggers an update of the registers
  *            @arg TIM_SlaveMode_Gated:     The counter clock is enabled when the trigger signal (TRGI) is high
  *            @arg TIM_SlaveMode_Trigger:   The counter starts at a rising edge of the trigger TRGI
  *            @arg TIM_SlaveMode_External1: Rising edges of the selected trigger (TRGI) clock the counter
  * @retval None
  */
void TIM_SelectSlaveMode(TIM_TypeDef* TIMx, uint16_t TIM_SlaveMode)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));
  assert_param(IS_TIM_SLAVE_MODE(TIM_SlaveMode));

  /* Reset the SMS Bits */
  TIMx->SMCR &= (uint16_t)~TIM_SMCR_SMS;

  /* Select the Slave Mode */
  TIMx->SMCR |= TIM_SlaveMode;
}

/**
  * @brief  Sets or Resets the TIMx Master/Slave Mode.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM peripheral.
  * @param  TIM_MasterSlaveMode: specifies the Timer Master Slave Mode.
  *          This parameter can be one of the following values:
  *            @arg TIM_MasterSlaveMode_Enable: synchronization between the current timer
  *                                             and its slaves (through TRGO)
  *            @arg TIM_MasterSlaveMode_Disable: No action
  * @retval None
  */
void TIM_SelectMasterSlaveMode(TIM_TypeDef* TIMx, uint16_t TIM_MasterSlaveMode)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));
  assert_param(IS_TIM_MSM_STATE(TIM_MasterSlaveMode));

  /* Reset the MSM Bit */
  TIMx->SMCR &= (uint16_t)~TIM_SMCR_MSM;
  
  /* Set or Reset the MSM Bit */
  TIMx->SMCR |= TIM_MasterSlaveMode;
}

/**
  * @brief  Configures the TIMx External Trigger (ETR).
  * @param  TIMx: where x can be  1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_ExtTRGPrescaler: The external Trigger Prescaler.
  *          This parameter can be one of the following values:
  *            @arg TIM_ExtTRGPSC_OFF: ETRP Prescaler OFF.
  *            @arg TIM_ExtTRGPSC_DIV2: ETRP frequency divided by 2.
  *            @arg TIM_ExtTRGPSC_DIV4: ETRP frequency divided by 4.
  *            @arg TIM_ExtTRGPSC_DIV8: ETRP frequency divided by 8.
  * @param  TIM_ExtTRGPolarity: The external Trigger Polarity.
  *          This parameter can be one of the following values:
  *            @arg TIM_ExtTRGPolarity_Inverted: active low or falling edge active.
  *            @arg TIM_ExtTRGPolarity_NonInverted: active high or rising edge active.
  * @param  ExtTRGFilter: External Trigger Filter.
  *          This parameter must be a value between 0x00 and 0x0F
  * @retval None
  */
void TIM_ETRConfig(TIM_TypeDef* TIMx, uint16_t TIM_ExtTRGPrescaler,
                   uint16_t TIM_ExtTRGPolarity, uint16_t ExtTRGFilter)
{
  uint16_t tmpsmcr = 0;

  /* Check the parameters */
  assert_param(IS_TIM_LIST3_PERIPH(TIMx));
  assert_param(IS_TIM_EXT_PRESCALER(TIM_ExtTRGPrescaler));
  assert_param(IS_TIM_EXT_POLARITY(TIM_ExtTRGPolarity));
  assert_param(IS_TIM_EXT_FILTER(ExtTRGFilter));

  tmpsmcr = TIMx->SMCR;

  /* Reset the ETR Bits */
  tmpsmcr &= SMCR_ETR_MASK;

  /* Set the Prescaler, the Filter value and the Polarity */
  tmpsmcr |= (uint16_t)(TIM_ExtTRGPrescaler | (uint16_t)(TIM_ExtTRGPolarity | (uint16_t)(ExtTRGFilter << (uint16_t)8)));

  /* Write to TIMx SMCR */
  TIMx->SMCR = tmpsmcr;
}
/**
  * @}
  */

/** @defgroup TIM_Group8 Specific interface management functions
 *  @brief    Specific interface management functions 
 *
@verbatim   
 ===============================================================================
                    Specific interface management functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Configures the TIMx Encoder Interface.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  TIM_EncoderMode: specifies the TIMx Encoder Mode.
  *          This parameter can be one of the following values:
  *            @arg TIM_EncoderMode_TI1: Counter counts on TI1FP1 edge depending on TI2FP2 level.
  *            @arg TIM_EncoderMode_TI2: Counter counts on TI2FP2 edge depending on TI1FP1 level.
  *            @arg TIM_EncoderMode_TI12: Counter counts on both TI1FP1 and TI2FP2 edges depending
  *                                       on the level of the other input.
  * @param  TIM_IC1Polarity: specifies the IC1 Polarity
  *          This parameter can be one of the following values:
  *            @arg TIM_ICPolarity_Falling: IC Falling edge.
  *            @arg TIM_ICPolarity_Rising: IC Rising edge.
  * @param  TIM_IC2Polarity: specifies the IC2 Polarity
  *          This parameter can be one of the following values:
  *            @arg TIM_ICPolarity_Falling: IC Falling edge.
  *            @arg TIM_ICPolarity_Rising: IC Rising edge.
  * @retval None
  */
void TIM_EncoderInterfaceConfig(TIM_TypeDef* TIMx, uint16_t TIM_EncoderMode,
                                uint16_t TIM_IC1Polarity, uint16_t TIM_IC2Polarity)
{
  uint16_t tmpsmcr = 0;
  uint16_t tmpccmr1 = 0;
  uint16_t tmpccer = 0;
    
  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));
  assert_param(IS_TIM_ENCODER_MODE(TIM_EncoderMode));
  assert_param(IS_TIM_IC_POLARITY(TIM_IC1Polarity));
  assert_param(IS_TIM_IC_POLARITY(TIM_IC2Polarity));

  /* Get the TIMx SMCR register value */
  tmpsmcr = TIMx->SMCR;

  /* Get the TIMx CCMR1 register value */
  tmpccmr1 = TIMx->CCMR1;

  /* Get the TIMx CCER register value */
  tmpccer = TIMx->CCER;

  /* Set the encoder Mode */
  tmpsmcr &= (uint16_t)~TIM_SMCR_SMS;
  tmpsmcr |= TIM_EncoderMode;

  /* Select the Capture Compare 1 and the Capture Compare 2 as input */
  tmpccmr1 &= ((uint16_t)~TIM_CCMR1_CC1S) & ((uint16_t)~TIM_CCMR1_CC2S);
  tmpccmr1 |= TIM_CCMR1_CC1S_0 | TIM_CCMR1_CC2S_0;

  /* Set the TI1 and the TI2 Polarities */
  tmpccer &= ((uint16_t)~TIM_CCER_CC1P) & ((uint16_t)~TIM_CCER_CC2P);
  tmpccer |= (uint16_t)(TIM_IC1Polarity | (uint16_t)(TIM_IC2Polarity << (uint16_t)4));

  /* Write to TIMx SMCR */
  TIMx->SMCR = tmpsmcr;

  /* Write to TIMx CCMR1 */
  TIMx->CCMR1 = tmpccmr1;

  /* Write to TIMx CCER */
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Enables or disables the TIMx's Hall sensor interface.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  NewState: new state of the TIMx Hall sensor interface.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void TIM_SelectHallSensor(TIM_TypeDef* TIMx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_TIM_LIST2_PERIPH(TIMx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Set the TI1S Bit */
    TIMx->CR2 |= TIM_CR2_TI1S;
  }
  else
  {
    /* Reset the TI1S Bit */
    TIMx->CR2 &= (uint16_t)~TIM_CR2_TI1S;
  }
}
/**
  * @}
  */

/** @defgroup TIM_Group9 Specific remapping management function
 *  @brief   Specific remapping management function
 *
@verbatim   
 ===============================================================================
                     Specific remapping management function
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Configures the TIM2, TIM5 and TIM11 Remapping input capabilities.
  * @param  TIMx: where x can be 2, 5 or 11 to select the TIM peripheral.
  * @param  TIM_Remap: specifies the TIM input remapping source.
  *          This parameter can be one of the following values:
  *            @arg TIM2_TIM8_TRGO: TIM2 ITR1 input is connected to TIM8 Trigger output(default)
  *            @arg TIM2_ETH_PTP:   TIM2 ITR1 input is connected to ETH PTP trogger output.
  *            @arg TIM2_USBFS_SOF: TIM2 ITR1 input is connected to USB FS SOF. 
  *            @arg TIM2_USBHS_SOF: TIM2 ITR1 input is connected to USB HS SOF. 
  *            @arg TIM5_GPIO:      TIM5 CH4 input is connected to dedicated Timer pin(default)
  *            @arg TIM5_LSI:       TIM5 CH4 input is connected to LSI clock.
  *            @arg TIM5_LSE:       TIM5 CH4 input is connected to LSE clock.
  *            @arg TIM5_RTC:       TIM5 CH4 input is connected to RTC Output event.
  *            @arg TIM11_GPIO:     TIM11 CH4 input is connected to dedicated Timer pin(default) 
  *            @arg TIM11_HSE:      TIM11 CH4 input is connected to HSE_RTC clock
  *                                 (HSE divided by a programmable prescaler)  
  * @retval None
  */
void TIM_RemapConfig(TIM_TypeDef* TIMx, uint16_t TIM_Remap)
{
 /* Check the parameters */
  assert_param(IS_TIM_LIST6_PERIPH(TIMx));
  assert_param(IS_TIM_REMAP(TIM_Remap));

  /* Set the Timer remapping configuration */
  TIMx->OR =  TIM_Remap;
}
/**
  * @}
  */

/**
  * @brief  Configure the TI1 as Input.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13 or 14 
  *         to select the TIM peripheral.
  * @param  TIM_ICPolarity : The Input Polarity.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICPolarity_Rising
  *            @arg TIM_ICPolarity_Falling
  *            @arg TIM_ICPolarity_BothEdge  
  * @param  TIM_ICSelection: specifies the input to be used.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICSelection_DirectTI: TIM Input 1 is selected to be connected to IC1.
  *            @arg TIM_ICSelection_IndirectTI: TIM Input 1 is selected to be connected to IC2.
  *            @arg TIM_ICSelection_TRC: TIM Input 1 is selected to be connected to TRC.
  * @param  TIM_ICFilter: Specifies the Input Capture Filter.
  *          This parameter must be a value between 0x00 and 0x0F.
  * @retval None
  */
static void TI1_Config(TIM_TypeDef* TIMx, uint16_t TIM_ICPolarity, uint16_t TIM_ICSelection,
                       uint16_t TIM_ICFilter)
{
  uint16_t tmpccmr1 = 0, tmpccer = 0;

  /* Disable the Channel 1: Reset the CC1E Bit */
  TIMx->CCER &= (uint16_t)~TIM_CCER_CC1E;
  tmpccmr1 = TIMx->CCMR1;
  tmpccer = TIMx->CCER;

  /* Select the Input and set the filter */
  tmpccmr1 &= ((uint16_t)~TIM_CCMR1_CC1S) & ((uint16_t)~TIM_CCMR1_IC1F);
  tmpccmr1 |= (uint16_t)(TIM_ICSelection | (uint16_t)(TIM_ICFilter << (uint16_t)4));

  /* Select the Polarity and set the CC1E Bit */
  tmpccer &= (uint16_t)~(TIM_CCER_CC1P | TIM_CCER_CC1NP);
  tmpccer |= (uint16_t)(TIM_ICPolarity | (uint16_t)TIM_CCER_CC1E);

  /* Write to TIMx CCMR1 and CCER registers */
  TIMx->CCMR1 = tmpccmr1;
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Configure the TI2 as Input.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5, 8, 9 or 12 to select the TIM 
  *         peripheral.
  * @param  TIM_ICPolarity : The Input Polarity.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICPolarity_Rising
  *            @arg TIM_ICPolarity_Falling
  *            @arg TIM_ICPolarity_BothEdge   
  * @param  TIM_ICSelection: specifies the input to be used.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICSelection_DirectTI: TIM Input 2 is selected to be connected to IC2.
  *            @arg TIM_ICSelection_IndirectTI: TIM Input 2 is selected to be connected to IC1.
  *            @arg TIM_ICSelection_TRC: TIM Input 2 is selected to be connected to TRC.
  * @param  TIM_ICFilter: Specifies the Input Capture Filter.
  *          This parameter must be a value between 0x00 and 0x0F.
  * @retval None
  */
static void TI2_Config(TIM_TypeDef* TIMx, uint16_t TIM_ICPolarity, uint16_t TIM_ICSelection,
                       uint16_t TIM_ICFilter)
{
  uint16_t tmpccmr1 = 0, tmpccer = 0, tmp = 0;

  /* Disable the Channel 2: Reset the CC2E Bit */
  TIMx->CCER &= (uint16_t)~TIM_CCER_CC2E;
  tmpccmr1 = TIMx->CCMR1;
  tmpccer = TIMx->CCER;
  tmp = (uint16_t)(TIM_ICPolarity << 4);

  /* Select the Input and set the filter */
  tmpccmr1 &= ((uint16_t)~TIM_CCMR1_CC2S) & ((uint16_t)~TIM_CCMR1_IC2F);
  tmpccmr1 |= (uint16_t)(TIM_ICFilter << 12);
  tmpccmr1 |= (uint16_t)(TIM_ICSelection << 8);

  /* Select the Polarity and set the CC2E Bit */
  tmpccer &= (uint16_t)~(TIM_CCER_CC2P | TIM_CCER_CC2NP);
  tmpccer |=  (uint16_t)(tmp | (uint16_t)TIM_CCER_CC2E);

  /* Write to TIMx CCMR1 and CCER registers */
  TIMx->CCMR1 = tmpccmr1 ;
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Configure the TI3 as Input.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_ICPolarity : The Input Polarity.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICPolarity_Rising
  *            @arg TIM_ICPolarity_Falling
  *            @arg TIM_ICPolarity_BothEdge         
  * @param  TIM_ICSelection: specifies the input to be used.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICSelection_DirectTI: TIM Input 3 is selected to be connected to IC3.
  *            @arg TIM_ICSelection_IndirectTI: TIM Input 3 is selected to be connected to IC4.
  *            @arg TIM_ICSelection_TRC: TIM Input 3 is selected to be connected to TRC.
  * @param  TIM_ICFilter: Specifies the Input Capture Filter.
  *          This parameter must be a value between 0x00 and 0x0F.
  * @retval None
  */
static void TI3_Config(TIM_TypeDef* TIMx, uint16_t TIM_ICPolarity, uint16_t TIM_ICSelection,
                       uint16_t TIM_ICFilter)
{
  uint16_t tmpccmr2 = 0, tmpccer = 0, tmp = 0;

  /* Disable the Channel 3: Reset the CC3E Bit */
  TIMx->CCER &= (uint16_t)~TIM_CCER_CC3E;
  tmpccmr2 = TIMx->CCMR2;
  tmpccer = TIMx->CCER;
  tmp = (uint16_t)(TIM_ICPolarity << 8);

  /* Select the Input and set the filter */
  tmpccmr2 &= ((uint16_t)~TIM_CCMR1_CC1S) & ((uint16_t)~TIM_CCMR2_IC3F);
  tmpccmr2 |= (uint16_t)(TIM_ICSelection | (uint16_t)(TIM_ICFilter << (uint16_t)4));

  /* Select the Polarity and set the CC3E Bit */
  tmpccer &= (uint16_t)~(TIM_CCER_CC3P | TIM_CCER_CC3NP);
  tmpccer |= (uint16_t)(tmp | (uint16_t)TIM_CCER_CC3E);

  /* Write to TIMx CCMR2 and CCER registers */
  TIMx->CCMR2 = tmpccmr2;
  TIMx->CCER = tmpccer;
}

/**
  * @brief  Configure the TI4 as Input.
  * @param  TIMx: where x can be 1, 2, 3, 4, 5 or 8 to select the TIM peripheral.
  * @param  TIM_ICPolarity : The Input Polarity.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICPolarity_Rising
  *            @arg TIM_ICPolarity_Falling
  *            @arg TIM_ICPolarity_BothEdge     
  * @param  TIM_ICSelection: specifies the input to be used.
  *          This parameter can be one of the following values:
  *            @arg TIM_ICSelection_DirectTI: TIM Input 4 is selected to be connected to IC4.
  *            @arg TIM_ICSelection_IndirectTI: TIM Input 4 is selected to be connected to IC3.
  *            @arg TIM_ICSelection_TRC: TIM Input 4 is selected to be connected to TRC.
  * @param  TIM_ICFilter: Specifies the Input Capture Filter.
  *          This parameter must be a value between 0x00 and 0x0F.
  * @retval None
  */
static void TI4_Config(TIM_TypeDef* TIMx, uint16_t TIM_ICPolarity, uint16_t TIM_ICSelection,
                       uint16_t TIM_ICFilter)
{
  uint16_t tmpccmr2 = 0, tmpccer = 0, tmp = 0;

  /* Disable the Channel 4: Reset the CC4E Bit */
  TIMx->CCER &= (uint16_t)~TIM_CCER_CC4E;
  tmpccmr2 = TIMx->CCMR2;
  tmpccer = TIMx->CCER;
  tmp = (uint16_t)(TIM_ICPolarity << 12);

  /* Select the Input and set the filter */
  tmpccmr2 &= ((uint16_t)~TIM_CCMR1_CC2S) & ((uint16_t)~TIM_CCMR1_IC2F);
  tmpccmr2 |= (uint16_t)(TIM_ICSelection << 8);
  tmpccmr2 |= (uint16_t)(TIM_ICFilter << 12);

  /* Select the Polarity and set the CC4E Bit */
  tmpccer &= (uint16_t)~(TIM_CCER_CC4P | TIM_CCER_CC4NP);
  tmpccer |= (uint16_t)(tmp | (uint16_t)TIM_CCER_CC4E);

  /* Write to TIMx CCMR2 and CCER registers */
  TIMx->CCMR2 = tmpccmr2;
  TIMx->CCER = tmpccer ;
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

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
