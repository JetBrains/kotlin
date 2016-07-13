/**
  ******************************************************************************
  * @file    stm32f4xx_adc.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the Analog to Digital Convertor (ADC) peripheral:
  *           - Initialization and Configuration (in addition to ADC multi mode 
  *             selection)
  *           - Analog Watchdog configuration
  *           - Temperature Sensor & Vrefint (Voltage Reference internal) & VBAT
  *             management 
  *           - Regular Channels Configuration
  *           - Regular Channels DMA Configuration
  *           - Injected channels Configuration
  *           - Interrupts and flags management
  *         
  *  @verbatim
  *
  *          ===================================================================
  *                                   How to use this driver
  *          ===================================================================

  *          1.  Enable the ADC interface clock using 
  *                  RCC_APB2PeriphClockCmd(RCC_APB2Periph_ADCx, ENABLE); 
  *     
  *          2. ADC pins configuration
  *               - Enable the clock for the ADC GPIOs using the following function:
  *                   RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOx, ENABLE);   
  *                - Configure these ADC pins in analog mode using GPIO_Init();  
  *
  *          3. Configure the ADC Prescaler, conversion resolution and data 
  *              alignment using the ADC_Init() function.
  *          4. Activate the ADC peripheral using ADC_Cmd() function.
  *
  *          Regular channels group configuration
  *          ====================================    
  *            - To configure the ADC regular channels group features, use 
  *              ADC_Init() and ADC_RegularChannelConfig() functions.
  *            - To activate the continuous mode, use the ADC_continuousModeCmd()
  *              function.
  *            - To configurate and activate the Discontinuous mode, use the 
  *              ADC_DiscModeChannelCountConfig() and ADC_DiscModeCmd() functions.
  *            - To read the ADC converted values, use the ADC_GetConversionValue()
  *              function.
  *
  *          Multi mode ADCs Regular channels configuration
  *          ===============================================
  *            - Refer to "Regular channels group configuration" description to
  *              configure the ADC1, ADC2 and ADC3 regular channels.        
  *            - Select the Multi mode ADC regular channels features (dual or 
  *              triple mode) using ADC_CommonInit() function and configure 
  *              the DMA mode using ADC_MultiModeDMARequestAfterLastTransferCmd() 
  *              functions.        
  *            - Read the ADCs converted values using the 
  *              ADC_GetMultiModeConversionValue() function.
  *
  *          DMA for Regular channels group features configuration
  *          ====================================================== 
  *           - To enable the DMA mode for regular channels group, use the 
  *             ADC_DMACmd() function.
  *           - To enable the generation of DMA requests continuously at the end
  *             of the last DMA transfer, use the ADC_DMARequestAfterLastTransferCmd() 
  *             function.
  *
  *          Injected channels group configuration
  *          =====================================    
  *            - To configure the ADC Injected channels group features, use 
  *              ADC_InjectedChannelConfig() and  ADC_InjectedSequencerLengthConfig()
  *              functions.
  *            - To activate the continuous mode, use the ADC_continuousModeCmd()
  *              function.
  *            - To activate the Injected Discontinuous mode, use the 
  *              ADC_InjectedDiscModeCmd() function.  
  *            - To activate the AutoInjected mode, use the ADC_AutoInjectedConvCmd() 
  *              function.        
  *            - To read the ADC converted values, use the ADC_GetInjectedConversionValue() 
  *              function.
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
#include "stm32f4xx_adc.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup ADC 
  * @brief ADC driver modules
  * @{
  */ 

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/ 

/* ADC DISCNUM mask */
#define CR1_DISCNUM_RESET         ((uint32_t)0xFFFF1FFF)

/* ADC AWDCH mask */
#define CR1_AWDCH_RESET           ((uint32_t)0xFFFFFFE0)   

/* ADC Analog watchdog enable mode mask */
#define CR1_AWDMode_RESET         ((uint32_t)0xFF3FFDFF)   

/* CR1 register Mask */
#define CR1_CLEAR_MASK            ((uint32_t)0xFCFFFEFF)

/* ADC EXTEN mask */
#define CR2_EXTEN_RESET           ((uint32_t)0xCFFFFFFF)  

/* ADC JEXTEN mask */
#define CR2_JEXTEN_RESET          ((uint32_t)0xFFCFFFFF)  

/* ADC JEXTSEL mask */
#define CR2_JEXTSEL_RESET         ((uint32_t)0xFFF0FFFF)  

/* CR2 register Mask */
#define CR2_CLEAR_MASK            ((uint32_t)0xC0FFF7FD)

/* ADC SQx mask */
#define SQR3_SQ_SET               ((uint32_t)0x0000001F)  
#define SQR2_SQ_SET               ((uint32_t)0x0000001F)  
#define SQR1_SQ_SET               ((uint32_t)0x0000001F)  

/* ADC L Mask */
#define SQR1_L_RESET              ((uint32_t)0xFF0FFFFF) 

/* ADC JSQx mask */
#define JSQR_JSQ_SET              ((uint32_t)0x0000001F) 

/* ADC JL mask */
#define JSQR_JL_SET               ((uint32_t)0x00300000) 
#define JSQR_JL_RESET             ((uint32_t)0xFFCFFFFF) 

/* ADC SMPx mask */
#define SMPR1_SMP_SET             ((uint32_t)0x00000007)  
#define SMPR2_SMP_SET             ((uint32_t)0x00000007) 

/* ADC JDRx registers offset */
#define JDR_OFFSET                ((uint8_t)0x28) 

/* ADC CDR register base address */
#define CDR_ADDRESS               ((uint32_t)0x40012308)   

/* ADC CCR register Mask */
#define CR_CLEAR_MASK             ((uint32_t)0xFFFC30E0)  

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup ADC_Private_Functions
  * @{
  */ 

/** @defgroup ADC_Group1 Initialization and Configuration functions
 *  @brief    Initialization and Configuration functions 
 *
@verbatim    
 ===============================================================================
                      Initialization and Configuration functions
 ===============================================================================  
  This section provides functions allowing to:
   - Initialize and configure the ADC Prescaler
   - ADC Conversion Resolution (12bit..6bit)
   - Scan Conversion Mode (multichannels or one channel) for regular group
   - ADC Continuous Conversion Mode (Continuous or Single conversion) for 
     regular group
   - External trigger Edge and source of regular group, 
   - Converted data alignment (left or right)
   - The number of ADC conversions that will be done using the sequencer for 
     regular channel group
   - Multi ADC mode selection
   - Direct memory access mode selection for multi ADC mode  
   - Delay between 2 sampling phases (used in dual or triple interleaved modes)
   - Enable or disable the ADC peripheral
   
@endverbatim
  * @{
  */

/**
  * @brief  Deinitializes all ADCs peripherals registers to their default reset 
  *         values.
  * @param  None
  * @retval None
  */
void ADC_DeInit(void)
{
  /* Enable all ADCs reset state */
  RCC_APB2PeriphResetCmd(RCC_APB2Periph_ADC, ENABLE);
  
  /* Release all ADCs from reset state */
  RCC_APB2PeriphResetCmd(RCC_APB2Periph_ADC, DISABLE);
}

/**
  * @brief  Initializes the ADCx peripheral according to the specified parameters 
  *         in the ADC_InitStruct.
  * @note   This function is used to configure the global features of the ADC ( 
  *         Resolution and Data Alignment), however, the rest of the configuration
  *         parameters are specific to the regular channels group (scan mode 
  *         activation, continuous mode activation, External trigger source and 
  *         edge, number of conversion in the regular channels group sequencer).  
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_InitStruct: pointer to an ADC_InitTypeDef structure that contains
  *         the configuration information for the specified ADC peripheral.
  * @retval None
  */
void ADC_Init(ADC_TypeDef* ADCx, ADC_InitTypeDef* ADC_InitStruct)
{
  uint32_t tmpreg1 = 0;
  uint8_t tmpreg2 = 0;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_RESOLUTION(ADC_InitStruct->ADC_Resolution)); 
  assert_param(IS_FUNCTIONAL_STATE(ADC_InitStruct->ADC_ScanConvMode));
  assert_param(IS_FUNCTIONAL_STATE(ADC_InitStruct->ADC_ContinuousConvMode)); 
  assert_param(IS_ADC_EXT_TRIG_EDGE(ADC_InitStruct->ADC_ExternalTrigConvEdge)); 
  assert_param(IS_ADC_EXT_TRIG(ADC_InitStruct->ADC_ExternalTrigConv));    
  assert_param(IS_ADC_DATA_ALIGN(ADC_InitStruct->ADC_DataAlign)); 
  assert_param(IS_ADC_REGULAR_LENGTH(ADC_InitStruct->ADC_NbrOfConversion));
  
  /*---------------------------- ADCx CR1 Configuration -----------------*/
  /* Get the ADCx CR1 value */
  tmpreg1 = ADCx->CR1;
  
  /* Clear RES and SCAN bits */
  tmpreg1 &= CR1_CLEAR_MASK;
  
  /* Configure ADCx: scan conversion mode and resolution */
  /* Set SCAN bit according to ADC_ScanConvMode value */
  /* Set RES bit according to ADC_Resolution value */ 
  tmpreg1 |= (uint32_t)(((uint32_t)ADC_InitStruct->ADC_ScanConvMode << 8) | \
                                   ADC_InitStruct->ADC_Resolution);
  /* Write to ADCx CR1 */
  ADCx->CR1 = tmpreg1;
  /*---------------------------- ADCx CR2 Configuration -----------------*/
  /* Get the ADCx CR2 value */
  tmpreg1 = ADCx->CR2;
  
  /* Clear CONT, ALIGN, EXTEN and EXTSEL bits */
  tmpreg1 &= CR2_CLEAR_MASK;
  
  /* Configure ADCx: external trigger event and edge, data alignment and 
     continuous conversion mode */
  /* Set ALIGN bit according to ADC_DataAlign value */
  /* Set EXTEN bits according to ADC_ExternalTrigConvEdge value */ 
  /* Set EXTSEL bits according to ADC_ExternalTrigConv value */
  /* Set CONT bit according to ADC_ContinuousConvMode value */
  tmpreg1 |= (uint32_t)(ADC_InitStruct->ADC_DataAlign | \
                        ADC_InitStruct->ADC_ExternalTrigConv | 
                        ADC_InitStruct->ADC_ExternalTrigConvEdge | \
                        ((uint32_t)ADC_InitStruct->ADC_ContinuousConvMode << 1));
                        
  /* Write to ADCx CR2 */
  ADCx->CR2 = tmpreg1;
  /*---------------------------- ADCx SQR1 Configuration -----------------*/
  /* Get the ADCx SQR1 value */
  tmpreg1 = ADCx->SQR1;
  
  /* Clear L bits */
  tmpreg1 &= SQR1_L_RESET;
  
  /* Configure ADCx: regular channel sequence length */
  /* Set L bits according to ADC_NbrOfConversion value */
  tmpreg2 |= (uint8_t)(ADC_InitStruct->ADC_NbrOfConversion - (uint8_t)1);
  tmpreg1 |= ((uint32_t)tmpreg2 << 20);
  
  /* Write to ADCx SQR1 */
  ADCx->SQR1 = tmpreg1;
}

/**
  * @brief  Fills each ADC_InitStruct member with its default value.
  * @note   This function is used to initialize the global features of the ADC ( 
  *         Resolution and Data Alignment), however, the rest of the configuration
  *         parameters are specific to the regular channels group (scan mode 
  *         activation, continuous mode activation, External trigger source and 
  *         edge, number of conversion in the regular channels group sequencer).  
  * @param  ADC_InitStruct: pointer to an ADC_InitTypeDef structure which will 
  *         be initialized.
  * @retval None
  */
void ADC_StructInit(ADC_InitTypeDef* ADC_InitStruct)
{
  /* Initialize the ADC_Mode member */
  ADC_InitStruct->ADC_Resolution = ADC_Resolution_12b;

  /* initialize the ADC_ScanConvMode member */
  ADC_InitStruct->ADC_ScanConvMode = DISABLE;

  /* Initialize the ADC_ContinuousConvMode member */
  ADC_InitStruct->ADC_ContinuousConvMode = DISABLE;

  /* Initialize the ADC_ExternalTrigConvEdge member */
  ADC_InitStruct->ADC_ExternalTrigConvEdge = ADC_ExternalTrigConvEdge_None;

  /* Initialize the ADC_ExternalTrigConv member */
  ADC_InitStruct->ADC_ExternalTrigConv = ADC_ExternalTrigConv_T1_CC1;

  /* Initialize the ADC_DataAlign member */
  ADC_InitStruct->ADC_DataAlign = ADC_DataAlign_Right;

  /* Initialize the ADC_NbrOfConversion member */
  ADC_InitStruct->ADC_NbrOfConversion = 1;
}

/**
  * @brief  Initializes the ADCs peripherals according to the specified parameters 
  *         in the ADC_CommonInitStruct.
  * @param  ADC_CommonInitStruct: pointer to an ADC_CommonInitTypeDef structure 
  *         that contains the configuration information for  All ADCs peripherals.
  * @retval None
  */
void ADC_CommonInit(ADC_CommonInitTypeDef* ADC_CommonInitStruct)
{
  uint32_t tmpreg1 = 0;
  /* Check the parameters */
  assert_param(IS_ADC_MODE(ADC_CommonInitStruct->ADC_Mode));
  assert_param(IS_ADC_PRESCALER(ADC_CommonInitStruct->ADC_Prescaler));
  assert_param(IS_ADC_DMA_ACCESS_MODE(ADC_CommonInitStruct->ADC_DMAAccessMode));
  assert_param(IS_ADC_SAMPLING_DELAY(ADC_CommonInitStruct->ADC_TwoSamplingDelay));
  /*---------------------------- ADC CCR Configuration -----------------*/
  /* Get the ADC CCR value */
  tmpreg1 = ADC->CCR;
  
  /* Clear MULTI, DELAY, DMA and ADCPRE bits */
  tmpreg1 &= CR_CLEAR_MASK;
  
  /* Configure ADCx: Multi mode, Delay between two sampling time, ADC prescaler,
     and DMA access mode for multimode */
  /* Set MULTI bits according to ADC_Mode value */
  /* Set ADCPRE bits according to ADC_Prescaler value */
  /* Set DMA bits according to ADC_DMAAccessMode value */
  /* Set DELAY bits according to ADC_TwoSamplingDelay value */    
  tmpreg1 |= (uint32_t)(ADC_CommonInitStruct->ADC_Mode | 
                        ADC_CommonInitStruct->ADC_Prescaler | 
                        ADC_CommonInitStruct->ADC_DMAAccessMode | 
                        ADC_CommonInitStruct->ADC_TwoSamplingDelay);
                        
  /* Write to ADC CCR */
  ADC->CCR = tmpreg1;
}

/**
  * @brief  Fills each ADC_CommonInitStruct member with its default value.
  * @param  ADC_CommonInitStruct: pointer to an ADC_CommonInitTypeDef structure
  *         which will be initialized.
  * @retval None
  */
void ADC_CommonStructInit(ADC_CommonInitTypeDef* ADC_CommonInitStruct)
{
  /* Initialize the ADC_Mode member */
  ADC_CommonInitStruct->ADC_Mode = ADC_Mode_Independent;

  /* initialize the ADC_Prescaler member */
  ADC_CommonInitStruct->ADC_Prescaler = ADC_Prescaler_Div2;

  /* Initialize the ADC_DMAAccessMode member */
  ADC_CommonInitStruct->ADC_DMAAccessMode = ADC_DMAAccessMode_Disabled;

  /* Initialize the ADC_TwoSamplingDelay member */
  ADC_CommonInitStruct->ADC_TwoSamplingDelay = ADC_TwoSamplingDelay_5Cycles;
}

/**
  * @brief  Enables or disables the specified ADC peripheral.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  NewState: new state of the ADCx peripheral. 
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void ADC_Cmd(ADC_TypeDef* ADCx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Set the ADON bit to wake up the ADC from power down mode */
    ADCx->CR2 |= (uint32_t)ADC_CR2_ADON;
  }
  else
  {
    /* Disable the selected ADC peripheral */
    ADCx->CR2 &= (uint32_t)(~ADC_CR2_ADON);
  }
}
/**
  * @}
  */

/** @defgroup ADC_Group2 Analog Watchdog configuration functions
 *  @brief    Analog Watchdog configuration functions 
 *
@verbatim   
 ===============================================================================
                    Analog Watchdog configuration functions
 ===============================================================================  

  This section provides functions allowing to configure the Analog Watchdog
  (AWD) feature in the ADC.
  
  A typical configuration Analog Watchdog is done following these steps :
   1. the ADC guarded channel(s) is (are) selected using the 
      ADC_AnalogWatchdogSingleChannelConfig() function.
   2. The Analog watchdog lower and higher threshold are configured using the  
     ADC_AnalogWatchdogThresholdsConfig() function.
   3. The Analog watchdog is enabled and configured to enable the check, on one
      or more channels, using the  ADC_AnalogWatchdogCmd() function.

@endverbatim
  * @{
  */
  
/**
  * @brief  Enables or disables the analog watchdog on single/all regular or 
  *         injected channels
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_AnalogWatchdog: the ADC analog watchdog configuration.
  *         This parameter can be one of the following values:
  *            @arg ADC_AnalogWatchdog_SingleRegEnable: Analog watchdog on a single regular channel
  *            @arg ADC_AnalogWatchdog_SingleInjecEnable: Analog watchdog on a single injected channel
  *            @arg ADC_AnalogWatchdog_SingleRegOrInjecEnable: Analog watchdog on a single regular or injected channel
  *            @arg ADC_AnalogWatchdog_AllRegEnable: Analog watchdog on all regular channel
  *            @arg ADC_AnalogWatchdog_AllInjecEnable: Analog watchdog on all injected channel
  *            @arg ADC_AnalogWatchdog_AllRegAllInjecEnable: Analog watchdog on all regular and injected channels
  *            @arg ADC_AnalogWatchdog_None: No channel guarded by the analog watchdog
  * @retval None	  
  */
void ADC_AnalogWatchdogCmd(ADC_TypeDef* ADCx, uint32_t ADC_AnalogWatchdog)
{
  uint32_t tmpreg = 0;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_ANALOG_WATCHDOG(ADC_AnalogWatchdog));
  
  /* Get the old register value */
  tmpreg = ADCx->CR1;
  
  /* Clear AWDEN, JAWDEN and AWDSGL bits */
  tmpreg &= CR1_AWDMode_RESET;
  
  /* Set the analog watchdog enable mode */
  tmpreg |= ADC_AnalogWatchdog;
  
  /* Store the new register value */
  ADCx->CR1 = tmpreg;
}

/**
  * @brief  Configures the high and low thresholds of the analog watchdog.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  HighThreshold: the ADC analog watchdog High threshold value.
  *          This parameter must be a 12-bit value.
  * @param  LowThreshold:  the ADC analog watchdog Low threshold value.
  *          This parameter must be a 12-bit value.
  * @retval None
  */
void ADC_AnalogWatchdogThresholdsConfig(ADC_TypeDef* ADCx, uint16_t HighThreshold,
                                        uint16_t LowThreshold)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_THRESHOLD(HighThreshold));
  assert_param(IS_ADC_THRESHOLD(LowThreshold));
  
  /* Set the ADCx high threshold */
  ADCx->HTR = HighThreshold;
  
  /* Set the ADCx low threshold */
  ADCx->LTR = LowThreshold;
}

/**
  * @brief  Configures the analog watchdog guarded single channel
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_Channel: the ADC channel to configure for the analog watchdog. 
  *          This parameter can be one of the following values:
  *            @arg ADC_Channel_0: ADC Channel0 selected
  *            @arg ADC_Channel_1: ADC Channel1 selected
  *            @arg ADC_Channel_2: ADC Channel2 selected
  *            @arg ADC_Channel_3: ADC Channel3 selected
  *            @arg ADC_Channel_4: ADC Channel4 selected
  *            @arg ADC_Channel_5: ADC Channel5 selected
  *            @arg ADC_Channel_6: ADC Channel6 selected
  *            @arg ADC_Channel_7: ADC Channel7 selected
  *            @arg ADC_Channel_8: ADC Channel8 selected
  *            @arg ADC_Channel_9: ADC Channel9 selected
  *            @arg ADC_Channel_10: ADC Channel10 selected
  *            @arg ADC_Channel_11: ADC Channel11 selected
  *            @arg ADC_Channel_12: ADC Channel12 selected
  *            @arg ADC_Channel_13: ADC Channel13 selected
  *            @arg ADC_Channel_14: ADC Channel14 selected
  *            @arg ADC_Channel_15: ADC Channel15 selected
  *            @arg ADC_Channel_16: ADC Channel16 selected
  *            @arg ADC_Channel_17: ADC Channel17 selected
  *            @arg ADC_Channel_18: ADC Channel18 selected
  * @retval None
  */
void ADC_AnalogWatchdogSingleChannelConfig(ADC_TypeDef* ADCx, uint8_t ADC_Channel)
{
  uint32_t tmpreg = 0;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_CHANNEL(ADC_Channel));
  
  /* Get the old register value */
  tmpreg = ADCx->CR1;
  
  /* Clear the Analog watchdog channel select bits */
  tmpreg &= CR1_AWDCH_RESET;
  
  /* Set the Analog watchdog channel */
  tmpreg |= ADC_Channel;
  
  /* Store the new register value */
  ADCx->CR1 = tmpreg;
}
/**
  * @}
  */

/** @defgroup ADC_Group3 Temperature Sensor, Vrefint (Voltage Reference internal) 
 *            and VBAT (Voltage BATtery) management functions
 *  @brief   Temperature Sensor, Vrefint and VBAT management functions 
 *
@verbatim   
 ===============================================================================
               Temperature Sensor, Vrefint and VBAT management functions
 ===============================================================================  

  This section provides functions allowing to enable/ disable the internal 
  connections between the ADC and the Temperature Sensor, the Vrefint and the
  Vbat sources.
     
  A typical configuration to get the Temperature sensor and Vrefint channels 
  voltages is done following these steps :
   1. Enable the internal connection of Temperature sensor and Vrefint sources 
      with the ADC channels using ADC_TempSensorVrefintCmd() function. 
   2. Select the ADC_Channel_TempSensor and/or ADC_Channel_Vrefint using 
      ADC_RegularChannelConfig() or  ADC_InjectedChannelConfig() functions 
   3. Get the voltage values, using ADC_GetConversionValue() or  
      ADC_GetInjectedConversionValue().

  A typical configuration to get the VBAT channel voltage is done following 
  these steps :
   1. Enable the internal connection of VBAT source with the ADC channel using 
      ADC_VBATCmd() function. 
   2. Select the ADC_Channel_Vbat using ADC_RegularChannelConfig() or  
      ADC_InjectedChannelConfig() functions 
   3. Get the voltage value, using ADC_GetConversionValue() or  
      ADC_GetInjectedConversionValue().
 
@endverbatim
  * @{
  */
  
  
/**
  * @brief  Enables or disables the temperature sensor and Vrefint channels.
  * @param  NewState: new state of the temperature sensor and Vrefint channels.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void ADC_TempSensorVrefintCmd(FunctionalState NewState)                
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the temperature sensor and Vrefint channel*/
    ADC->CCR |= (uint32_t)ADC_CCR_TSVREFE;
  }
  else
  {
    /* Disable the temperature sensor and Vrefint channel*/
    ADC->CCR &= (uint32_t)(~ADC_CCR_TSVREFE);
  }
}

/**
  * @brief  Enables or disables the VBAT (Voltage Battery) channel.
  * @param  NewState: new state of the VBAT channel.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void ADC_VBATCmd(FunctionalState NewState)                             
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the VBAT channel*/
    ADC->CCR |= (uint32_t)ADC_CCR_VBATE;
  }
  else
  {
    /* Disable the VBAT channel*/
    ADC->CCR &= (uint32_t)(~ADC_CCR_VBATE);
  }
}

/**
  * @}
  */

/** @defgroup ADC_Group4 Regular Channels Configuration functions
 *  @brief   Regular Channels Configuration functions 
 *
@verbatim   
 ===============================================================================
                  Regular Channels Configuration functions
 ===============================================================================  

  This section provides functions allowing to manage the ADC's regular channels,
  it is composed of 2 sub sections : 
  
  1. Configuration and management functions for regular channels: This subsection 
     provides functions allowing to configure the ADC regular channels :    
          - Configure the rank in the regular group sequencer for each channel
          - Configure the sampling time for each channel
          - select the conversion Trigger for regular channels
          - select the desired EOC event behavior configuration
          - Activate the continuous Mode  (*)
          - Activate the Discontinuous Mode 
     Please Note that the following features for regular channels are configurated
     using the ADC_Init() function : 
          - scan mode activation 
          - continuous mode activation (**) 
          - External trigger source  
          - External trigger edge 
          - number of conversion in the regular channels group sequencer.
     
     @note (*) and (**) are performing the same configuration
     
  2. Get the conversion data: This subsection provides an important function in 
     the ADC peripheral since it returns the converted data of the current 
     regular channel. When the Conversion value is read, the EOC Flag is 
     automatically cleared.
     
     @note For multi ADC mode, the last ADC1, ADC2 and ADC3 regular conversions 
           results data (in the selected multi mode) can be returned in the same 
           time using ADC_GetMultiModeConversionValue() function. 
       
  
@endverbatim
  * @{
  */
/**
  * @brief  Configures for the selected ADC regular channel its corresponding
  *         rank in the sequencer and its sample time.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_Channel: the ADC channel to configure. 
  *          This parameter can be one of the following values:
  *            @arg ADC_Channel_0: ADC Channel0 selected
  *            @arg ADC_Channel_1: ADC Channel1 selected
  *            @arg ADC_Channel_2: ADC Channel2 selected
  *            @arg ADC_Channel_3: ADC Channel3 selected
  *            @arg ADC_Channel_4: ADC Channel4 selected
  *            @arg ADC_Channel_5: ADC Channel5 selected
  *            @arg ADC_Channel_6: ADC Channel6 selected
  *            @arg ADC_Channel_7: ADC Channel7 selected
  *            @arg ADC_Channel_8: ADC Channel8 selected
  *            @arg ADC_Channel_9: ADC Channel9 selected
  *            @arg ADC_Channel_10: ADC Channel10 selected
  *            @arg ADC_Channel_11: ADC Channel11 selected
  *            @arg ADC_Channel_12: ADC Channel12 selected
  *            @arg ADC_Channel_13: ADC Channel13 selected
  *            @arg ADC_Channel_14: ADC Channel14 selected
  *            @arg ADC_Channel_15: ADC Channel15 selected
  *            @arg ADC_Channel_16: ADC Channel16 selected
  *            @arg ADC_Channel_17: ADC Channel17 selected
  *            @arg ADC_Channel_18: ADC Channel18 selected                       
  * @param  Rank: The rank in the regular group sequencer.
  *          This parameter must be between 1 to 16.
  * @param  ADC_SampleTime: The sample time value to be set for the selected channel. 
  *          This parameter can be one of the following values:
  *            @arg ADC_SampleTime_3Cycles: Sample time equal to 3 cycles
  *            @arg ADC_SampleTime_15Cycles: Sample time equal to 15 cycles
  *            @arg ADC_SampleTime_28Cycles: Sample time equal to 28 cycles
  *            @arg ADC_SampleTime_56Cycles: Sample time equal to 56 cycles	
  *            @arg ADC_SampleTime_84Cycles: Sample time equal to 84 cycles	
  *            @arg ADC_SampleTime_112Cycles: Sample time equal to 112 cycles	
  *            @arg ADC_SampleTime_144Cycles: Sample time equal to 144 cycles	
  *            @arg ADC_SampleTime_480Cycles: Sample time equal to 480 cycles	
  * @retval None
  */
void ADC_RegularChannelConfig(ADC_TypeDef* ADCx, uint8_t ADC_Channel, uint8_t Rank, uint8_t ADC_SampleTime)
{
  uint32_t tmpreg1 = 0, tmpreg2 = 0;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_CHANNEL(ADC_Channel));
  assert_param(IS_ADC_REGULAR_RANK(Rank));
  assert_param(IS_ADC_SAMPLE_TIME(ADC_SampleTime));
  
  /* if ADC_Channel_10 ... ADC_Channel_18 is selected */
  if (ADC_Channel > ADC_Channel_9)
  {
    /* Get the old register value */
    tmpreg1 = ADCx->SMPR1;
    
    /* Calculate the mask to clear */
    tmpreg2 = SMPR1_SMP_SET << (3 * (ADC_Channel - 10));
    
    /* Clear the old sample time */
    tmpreg1 &= ~tmpreg2;
    
    /* Calculate the mask to set */
    tmpreg2 = (uint32_t)ADC_SampleTime << (3 * (ADC_Channel - 10));
    
    /* Set the new sample time */
    tmpreg1 |= tmpreg2;
    
    /* Store the new register value */
    ADCx->SMPR1 = tmpreg1;
  }
  else /* ADC_Channel include in ADC_Channel_[0..9] */
  {
    /* Get the old register value */
    tmpreg1 = ADCx->SMPR2;
    
    /* Calculate the mask to clear */
    tmpreg2 = SMPR2_SMP_SET << (3 * ADC_Channel);
    
    /* Clear the old sample time */
    tmpreg1 &= ~tmpreg2;
    
    /* Calculate the mask to set */
    tmpreg2 = (uint32_t)ADC_SampleTime << (3 * ADC_Channel);
    
    /* Set the new sample time */
    tmpreg1 |= tmpreg2;
    
    /* Store the new register value */
    ADCx->SMPR2 = tmpreg1;
  }
  /* For Rank 1 to 6 */
  if (Rank < 7)
  {
    /* Get the old register value */
    tmpreg1 = ADCx->SQR3;
    
    /* Calculate the mask to clear */
    tmpreg2 = SQR3_SQ_SET << (5 * (Rank - 1));
    
    /* Clear the old SQx bits for the selected rank */
    tmpreg1 &= ~tmpreg2;
    
    /* Calculate the mask to set */
    tmpreg2 = (uint32_t)ADC_Channel << (5 * (Rank - 1));
    
    /* Set the SQx bits for the selected rank */
    tmpreg1 |= tmpreg2;
    
    /* Store the new register value */
    ADCx->SQR3 = tmpreg1;
  }
  /* For Rank 7 to 12 */
  else if (Rank < 13)
  {
    /* Get the old register value */
    tmpreg1 = ADCx->SQR2;
    
    /* Calculate the mask to clear */
    tmpreg2 = SQR2_SQ_SET << (5 * (Rank - 7));
    
    /* Clear the old SQx bits for the selected rank */
    tmpreg1 &= ~tmpreg2;
    
    /* Calculate the mask to set */
    tmpreg2 = (uint32_t)ADC_Channel << (5 * (Rank - 7));
    
    /* Set the SQx bits for the selected rank */
    tmpreg1 |= tmpreg2;
    
    /* Store the new register value */
    ADCx->SQR2 = tmpreg1;
  }
  /* For Rank 13 to 16 */
  else
  {
    /* Get the old register value */
    tmpreg1 = ADCx->SQR1;
    
    /* Calculate the mask to clear */
    tmpreg2 = SQR1_SQ_SET << (5 * (Rank - 13));
    
    /* Clear the old SQx bits for the selected rank */
    tmpreg1 &= ~tmpreg2;
    
    /* Calculate the mask to set */
    tmpreg2 = (uint32_t)ADC_Channel << (5 * (Rank - 13));
    
    /* Set the SQx bits for the selected rank */
    tmpreg1 |= tmpreg2;
    
    /* Store the new register value */
    ADCx->SQR1 = tmpreg1;
  }
}

/**
  * @brief  Enables the selected ADC software start conversion of the regular channels.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @retval None
  */
void ADC_SoftwareStartConv(ADC_TypeDef* ADCx)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  
  /* Enable the selected ADC conversion for regular group */
  ADCx->CR2 |= (uint32_t)ADC_CR2_SWSTART;
}

/**
  * @brief  Gets the selected ADC Software start regular conversion Status.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @retval The new state of ADC software start conversion (SET or RESET).
  */
FlagStatus ADC_GetSoftwareStartConvStatus(ADC_TypeDef* ADCx)
{
  FlagStatus bitstatus = RESET;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  
  /* Check the status of SWSTART bit */
  if ((ADCx->CR2 & ADC_CR2_JSWSTART) != (uint32_t)RESET)
  {
    /* SWSTART bit is set */
    bitstatus = SET;
  }
  else
  {
    /* SWSTART bit is reset */
    bitstatus = RESET;
  }
  
  /* Return the SWSTART bit status */
  return  bitstatus;
}


/**
  * @brief  Enables or disables the EOC on each regular channel conversion
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  NewState: new state of the selected ADC EOC flag rising
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void ADC_EOCOnEachRegularChannelCmd(ADC_TypeDef* ADCx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the selected ADC EOC rising on each regular channel conversion */
    ADCx->CR2 |= (uint32_t)ADC_CR2_EOCS;
  }
  else
  {
    /* Disable the selected ADC EOC rising on each regular channel conversion */
    ADCx->CR2 &= (uint32_t)(~ADC_CR2_EOCS);
  }
}

/**
  * @brief  Enables or disables the ADC continuous conversion mode 
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  NewState: new state of the selected ADC continuous conversion mode
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void ADC_ContinuousModeCmd(ADC_TypeDef* ADCx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the selected ADC continuous conversion mode */
    ADCx->CR2 |= (uint32_t)ADC_CR2_CONT;
  }
  else
  {
    /* Disable the selected ADC continuous conversion mode */
    ADCx->CR2 &= (uint32_t)(~ADC_CR2_CONT);
  }
}

/**
  * @brief  Configures the discontinuous mode for the selected ADC regular group 
  *         channel.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  Number: specifies the discontinuous mode regular channel count value.
  *          This number must be between 1 and 8.
  * @retval None
  */
void ADC_DiscModeChannelCountConfig(ADC_TypeDef* ADCx, uint8_t Number)
{
  uint32_t tmpreg1 = 0;
  uint32_t tmpreg2 = 0;
  
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_REGULAR_DISC_NUMBER(Number));
  
  /* Get the old register value */
  tmpreg1 = ADCx->CR1;
  
  /* Clear the old discontinuous mode channel count */
  tmpreg1 &= CR1_DISCNUM_RESET;
  
  /* Set the discontinuous mode channel count */
  tmpreg2 = Number - 1;
  tmpreg1 |= tmpreg2 << 13;
  
  /* Store the new register value */
  ADCx->CR1 = tmpreg1;
}

/**
  * @brief  Enables or disables the discontinuous mode on regular group channel 
  *         for the specified ADC
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  NewState: new state of the selected ADC discontinuous mode on 
  *         regular group channel.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void ADC_DiscModeCmd(ADC_TypeDef* ADCx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the selected ADC regular discontinuous mode */
    ADCx->CR1 |= (uint32_t)ADC_CR1_DISCEN;
  }
  else
  {
    /* Disable the selected ADC regular discontinuous mode */
    ADCx->CR1 &= (uint32_t)(~ADC_CR1_DISCEN);
  }
}

/**
  * @brief  Returns the last ADCx conversion result data for regular channel.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @retval The Data conversion value.
  */
uint16_t ADC_GetConversionValue(ADC_TypeDef* ADCx)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  
  /* Return the selected ADC conversion value */
  return (uint16_t) ADCx->DR;
}

/**
  * @brief  Returns the last ADC1, ADC2 and ADC3 regular conversions results 
  *         data in the selected multi mode.
  * @param  None  
  * @retval The Data conversion value.
  * @note   In dual mode, the value returned by this function is as following
  *           Data[15:0] : these bits contain the regular data of ADC1.
  *           Data[31:16]: these bits contain the regular data of ADC2.
  * @note   In triple mode, the value returned by this function is as following
  *           Data[15:0] : these bits contain alternatively the regular data of ADC1, ADC3 and ADC2.
  *           Data[31:16]: these bits contain alternatively the regular data of ADC2, ADC1 and ADC3.           
  */
uint32_t ADC_GetMultiModeConversionValue(void)
{
  /* Return the multi mode conversion value */
  return (*(__IO uint32_t *) CDR_ADDRESS);
}
/**
  * @}
  */

/** @defgroup ADC_Group5 Regular Channels DMA Configuration functions
 *  @brief   Regular Channels DMA Configuration functions 
 *
@verbatim   
 ===============================================================================
                   Regular Channels DMA Configuration functions
 ===============================================================================  

  This section provides functions allowing to configure the DMA for ADC regular 
  channels.
  Since converted regular channel values are stored into a unique data register, 
  it is useful to use DMA for conversion of more than one regular channel. This 
  avoids the loss of the data already stored in the ADC Data register. 
  
  When the DMA mode is enabled (using the ADC_DMACmd() function), after each
  conversion of a regular channel, a DMA request is generated.
  
  Depending on the "DMA disable selection for Independent ADC mode" 
  configuration (using the ADC_DMARequestAfterLastTransferCmd() function), 
  at the end of the last DMA transfer, two possibilities are allowed:
  - No new DMA request is issued to the DMA controller (feature DISABLED) 
  - Requests can continue to be generated (feature ENABLED).
  
  Depending on the "DMA disable selection for multi ADC mode" configuration 
  (using the void ADC_MultiModeDMARequestAfterLastTransferCmd() function), 
  at the end of the last DMA transfer, two possibilities are allowed:
  - No new DMA request is issued to the DMA controller (feature DISABLED) 
  - Requests can continue to be generated (feature ENABLED).

@endverbatim
  * @{
  */
  
 /**
  * @brief  Enables or disables the specified ADC DMA request.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  NewState: new state of the selected ADC DMA transfer.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void ADC_DMACmd(ADC_TypeDef* ADCx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the selected ADC DMA request */
    ADCx->CR2 |= (uint32_t)ADC_CR2_DMA;
  }
  else
  {
    /* Disable the selected ADC DMA request */
    ADCx->CR2 &= (uint32_t)(~ADC_CR2_DMA);
  }
}

/**
  * @brief  Enables or disables the ADC DMA request after last transfer (Single-ADC mode)  
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  NewState: new state of the selected ADC DMA request after last transfer.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void ADC_DMARequestAfterLastTransferCmd(ADC_TypeDef* ADCx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the selected ADC DMA request after last transfer */
    ADCx->CR2 |= (uint32_t)ADC_CR2_DDS;
  }
  else
  {
    /* Disable the selected ADC DMA request after last transfer */
    ADCx->CR2 &= (uint32_t)(~ADC_CR2_DDS);
  }
}

/**
  * @brief  Enables or disables the ADC DMA request after last transfer in multi ADC mode       
  * @param  NewState: new state of the selected ADC DMA request after last transfer.
  *          This parameter can be: ENABLE or DISABLE.
  * @note   if Enabled, DMA requests are issued as long as data are converted and 
  *         DMA mode for multi ADC mode (selected using ADC_CommonInit() function 
  *         by ADC_CommonInitStruct.ADC_DMAAccessMode structure member) is 
  *          ADC_DMAAccessMode_1, ADC_DMAAccessMode_2 or ADC_DMAAccessMode_3.     
  * @retval None
  */
void ADC_MultiModeDMARequestAfterLastTransferCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the selected ADC DMA request after last transfer */
    ADC->CCR |= (uint32_t)ADC_CCR_DDS;
  }
  else
  {
    /* Disable the selected ADC DMA request after last transfer */
    ADC->CCR &= (uint32_t)(~ADC_CCR_DDS);
  }
}
/**
  * @}
  */

/** @defgroup ADC_Group6 Injected channels Configuration functions
 *  @brief   Injected channels Configuration functions 
 *
@verbatim   
 ===============================================================================
                     Injected channels Configuration functions
 ===============================================================================  

  This section provide functions allowing to configure the ADC Injected channels,
  it is composed of 2 sub sections : 
    
  1. Configuration functions for Injected channels: This subsection provides 
     functions allowing to configure the ADC injected channels :    
    - Configure the rank in the injected group sequencer for each channel
    - Configure the sampling time for each channel    
    - Activate the Auto injected Mode  
    - Activate the Discontinuous Mode 
    - scan mode activation  
    - External/software trigger source   
    - External trigger edge 
    - injected channels sequencer.
    
   2. Get the Specified Injected channel conversion data: This subsection 
      provides an important function in the ADC peripheral since it returns the 
      converted data of the specific injected channel.

@endverbatim
  * @{
  */ 
/**
  * @brief  Configures for the selected ADC injected channel its corresponding
  *         rank in the sequencer and its sample time.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_Channel: the ADC channel to configure. 
  *          This parameter can be one of the following values:
  *            @arg ADC_Channel_0: ADC Channel0 selected
  *            @arg ADC_Channel_1: ADC Channel1 selected
  *            @arg ADC_Channel_2: ADC Channel2 selected
  *            @arg ADC_Channel_3: ADC Channel3 selected
  *            @arg ADC_Channel_4: ADC Channel4 selected
  *            @arg ADC_Channel_5: ADC Channel5 selected
  *            @arg ADC_Channel_6: ADC Channel6 selected
  *            @arg ADC_Channel_7: ADC Channel7 selected
  *            @arg ADC_Channel_8: ADC Channel8 selected
  *            @arg ADC_Channel_9: ADC Channel9 selected
  *            @arg ADC_Channel_10: ADC Channel10 selected
  *            @arg ADC_Channel_11: ADC Channel11 selected
  *            @arg ADC_Channel_12: ADC Channel12 selected
  *            @arg ADC_Channel_13: ADC Channel13 selected
  *            @arg ADC_Channel_14: ADC Channel14 selected
  *            @arg ADC_Channel_15: ADC Channel15 selected
  *            @arg ADC_Channel_16: ADC Channel16 selected
  *            @arg ADC_Channel_17: ADC Channel17 selected
  *            @arg ADC_Channel_18: ADC Channel18 selected                       
  * @param  Rank: The rank in the injected group sequencer. 
  *          This parameter must be between 1 to 4.
  * @param  ADC_SampleTime: The sample time value to be set for the selected channel. 
  *          This parameter can be one of the following values:
  *            @arg ADC_SampleTime_3Cycles: Sample time equal to 3 cycles
  *            @arg ADC_SampleTime_15Cycles: Sample time equal to 15 cycles
  *            @arg ADC_SampleTime_28Cycles: Sample time equal to 28 cycles
  *            @arg ADC_SampleTime_56Cycles: Sample time equal to 56 cycles	
  *            @arg ADC_SampleTime_84Cycles: Sample time equal to 84 cycles	
  *            @arg ADC_SampleTime_112Cycles: Sample time equal to 112 cycles	
  *            @arg ADC_SampleTime_144Cycles: Sample time equal to 144 cycles	
  *            @arg ADC_SampleTime_480Cycles: Sample time equal to 480 cycles	
  * @retval None
  */
void ADC_InjectedChannelConfig(ADC_TypeDef* ADCx, uint8_t ADC_Channel, uint8_t Rank, uint8_t ADC_SampleTime)
{
  uint32_t tmpreg1 = 0, tmpreg2 = 0, tmpreg3 = 0;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_CHANNEL(ADC_Channel));
  assert_param(IS_ADC_INJECTED_RANK(Rank));
  assert_param(IS_ADC_SAMPLE_TIME(ADC_SampleTime));
  /* if ADC_Channel_10 ... ADC_Channel_18 is selected */
  if (ADC_Channel > ADC_Channel_9)
  {
    /* Get the old register value */
    tmpreg1 = ADCx->SMPR1;
    /* Calculate the mask to clear */
    tmpreg2 = SMPR1_SMP_SET << (3*(ADC_Channel - 10));
    /* Clear the old sample time */
    tmpreg1 &= ~tmpreg2;
    /* Calculate the mask to set */
    tmpreg2 = (uint32_t)ADC_SampleTime << (3*(ADC_Channel - 10));
    /* Set the new sample time */
    tmpreg1 |= tmpreg2;
    /* Store the new register value */
    ADCx->SMPR1 = tmpreg1;
  }
  else /* ADC_Channel include in ADC_Channel_[0..9] */
  {
    /* Get the old register value */
    tmpreg1 = ADCx->SMPR2;
    /* Calculate the mask to clear */
    tmpreg2 = SMPR2_SMP_SET << (3 * ADC_Channel);
    /* Clear the old sample time */
    tmpreg1 &= ~tmpreg2;
    /* Calculate the mask to set */
    tmpreg2 = (uint32_t)ADC_SampleTime << (3 * ADC_Channel);
    /* Set the new sample time */
    tmpreg1 |= tmpreg2;
    /* Store the new register value */
    ADCx->SMPR2 = tmpreg1;
  }
  /* Rank configuration */
  /* Get the old register value */
  tmpreg1 = ADCx->JSQR;
  /* Get JL value: Number = JL+1 */
  tmpreg3 =  (tmpreg1 & JSQR_JL_SET)>> 20;
  /* Calculate the mask to clear: ((Rank-1)+(4-JL-1)) */
  tmpreg2 = JSQR_JSQ_SET << (5 * (uint8_t)((Rank + 3) - (tmpreg3 + 1)));
  /* Clear the old JSQx bits for the selected rank */
  tmpreg1 &= ~tmpreg2;
  /* Calculate the mask to set: ((Rank-1)+(4-JL-1)) */
  tmpreg2 = (uint32_t)ADC_Channel << (5 * (uint8_t)((Rank + 3) - (tmpreg3 + 1)));
  /* Set the JSQx bits for the selected rank */
  tmpreg1 |= tmpreg2;
  /* Store the new register value */
  ADCx->JSQR = tmpreg1;
}

/**
  * @brief  Configures the sequencer length for injected channels
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  Length: The sequencer length. 
  *          This parameter must be a number between 1 to 4.
  * @retval None
  */
void ADC_InjectedSequencerLengthConfig(ADC_TypeDef* ADCx, uint8_t Length)
{
  uint32_t tmpreg1 = 0;
  uint32_t tmpreg2 = 0;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_INJECTED_LENGTH(Length));
  
  /* Get the old register value */
  tmpreg1 = ADCx->JSQR;
  
  /* Clear the old injected sequence length JL bits */
  tmpreg1 &= JSQR_JL_RESET;
  
  /* Set the injected sequence length JL bits */
  tmpreg2 = Length - 1; 
  tmpreg1 |= tmpreg2 << 20;
  
  /* Store the new register value */
  ADCx->JSQR = tmpreg1;
}

/**
  * @brief  Set the injected channels conversion value offset
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_InjectedChannel: the ADC injected channel to set its offset. 
  *          This parameter can be one of the following values:
  *            @arg ADC_InjectedChannel_1: Injected Channel1 selected
  *            @arg ADC_InjectedChannel_2: Injected Channel2 selected
  *            @arg ADC_InjectedChannel_3: Injected Channel3 selected
  *            @arg ADC_InjectedChannel_4: Injected Channel4 selected
  * @param  Offset: the offset value for the selected ADC injected channel
  *          This parameter must be a 12bit value.
  * @retval None
  */
void ADC_SetInjectedOffset(ADC_TypeDef* ADCx, uint8_t ADC_InjectedChannel, uint16_t Offset)
{
    __IO uint32_t tmp = 0;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_INJECTED_CHANNEL(ADC_InjectedChannel));
  assert_param(IS_ADC_OFFSET(Offset));
  
  tmp = (uint32_t)ADCx;
  tmp += ADC_InjectedChannel;
  
  /* Set the selected injected channel data offset */
 *(__IO uint32_t *) tmp = (uint32_t)Offset;
}

 /**
  * @brief  Configures the ADCx external trigger for injected channels conversion.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_ExternalTrigInjecConv: specifies the ADC trigger to start injected conversion.
  *          This parameter can be one of the following values:                    
  *            @arg ADC_ExternalTrigInjecConv_T1_CC4: Timer1 capture compare4 selected 
  *            @arg ADC_ExternalTrigInjecConv_T1_TRGO: Timer1 TRGO event selected 
  *            @arg ADC_ExternalTrigInjecConv_T2_CC1: Timer2 capture compare1 selected 
  *            @arg ADC_ExternalTrigInjecConv_T2_TRGO: Timer2 TRGO event selected 
  *            @arg ADC_ExternalTrigInjecConv_T3_CC2: Timer3 capture compare2 selected 
  *            @arg ADC_ExternalTrigInjecConv_T3_CC4: Timer3 capture compare4 selected 
  *            @arg ADC_ExternalTrigInjecConv_T4_CC1: Timer4 capture compare1 selected                       
  *            @arg ADC_ExternalTrigInjecConv_T4_CC2: Timer4 capture compare2 selected 
  *            @arg ADC_ExternalTrigInjecConv_T4_CC3: Timer4 capture compare3 selected                        
  *            @arg ADC_ExternalTrigInjecConv_T4_TRGO: Timer4 TRGO event selected 
  *            @arg ADC_ExternalTrigInjecConv_T5_CC4: Timer5 capture compare4 selected                        
  *            @arg ADC_ExternalTrigInjecConv_T5_TRGO: Timer5 TRGO event selected                        
  *            @arg ADC_ExternalTrigInjecConv_T8_CC2: Timer8 capture compare2 selected
  *            @arg ADC_ExternalTrigInjecConv_T8_CC3: Timer8 capture compare3 selected                        
  *            @arg ADC_ExternalTrigInjecConv_T8_CC4: Timer8 capture compare4 selected 
  *            @arg ADC_ExternalTrigInjecConv_Ext_IT15: External interrupt line 15 event selected                          
  * @retval None
  */
void ADC_ExternalTrigInjectedConvConfig(ADC_TypeDef* ADCx, uint32_t ADC_ExternalTrigInjecConv)
{
  uint32_t tmpreg = 0;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_EXT_INJEC_TRIG(ADC_ExternalTrigInjecConv));
  
  /* Get the old register value */
  tmpreg = ADCx->CR2;
  
  /* Clear the old external event selection for injected group */
  tmpreg &= CR2_JEXTSEL_RESET;
  
  /* Set the external event selection for injected group */
  tmpreg |= ADC_ExternalTrigInjecConv;
  
  /* Store the new register value */
  ADCx->CR2 = tmpreg;
}

/**
  * @brief  Configures the ADCx external trigger edge for injected channels conversion.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_ExternalTrigInjecConvEdge: specifies the ADC external trigger edge
  *         to start injected conversion. 
  *          This parameter can be one of the following values:
  *            @arg ADC_ExternalTrigInjecConvEdge_None: external trigger disabled for 
  *                                                     injected conversion
  *            @arg ADC_ExternalTrigInjecConvEdge_Rising: detection on rising edge
  *            @arg ADC_ExternalTrigInjecConvEdge_Falling: detection on falling edge
  *            @arg ADC_ExternalTrigInjecConvEdge_RisingFalling: detection on both rising 
  *                                                               and falling edge
  * @retval None
  */
void ADC_ExternalTrigInjectedConvEdgeConfig(ADC_TypeDef* ADCx, uint32_t ADC_ExternalTrigInjecConvEdge)
{
  uint32_t tmpreg = 0;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_EXT_INJEC_TRIG_EDGE(ADC_ExternalTrigInjecConvEdge));
  /* Get the old register value */
  tmpreg = ADCx->CR2;
  /* Clear the old external trigger edge for injected group */
  tmpreg &= CR2_JEXTEN_RESET;
  /* Set the new external trigger edge for injected group */
  tmpreg |= ADC_ExternalTrigInjecConvEdge;
  /* Store the new register value */
  ADCx->CR2 = tmpreg;
}

/**
  * @brief  Enables the selected ADC software start conversion of the injected channels.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @retval None
  */
void ADC_SoftwareStartInjectedConv(ADC_TypeDef* ADCx)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  /* Enable the selected ADC conversion for injected group */
  ADCx->CR2 |= (uint32_t)ADC_CR2_JSWSTART;
}

/**
  * @brief  Gets the selected ADC Software start injected conversion Status.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @retval The new state of ADC software start injected conversion (SET or RESET).
  */
FlagStatus ADC_GetSoftwareStartInjectedConvCmdStatus(ADC_TypeDef* ADCx)
{
  FlagStatus bitstatus = RESET;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  
  /* Check the status of JSWSTART bit */
  if ((ADCx->CR2 & ADC_CR2_JSWSTART) != (uint32_t)RESET)
  {
    /* JSWSTART bit is set */
    bitstatus = SET;
  }
  else
  {
    /* JSWSTART bit is reset */
    bitstatus = RESET;
  }
  /* Return the JSWSTART bit status */
  return  bitstatus;
}

/**
  * @brief  Enables or disables the selected ADC automatic injected group 
  *         conversion after regular one.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  NewState: new state of the selected ADC auto injected conversion
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void ADC_AutoInjectedConvCmd(ADC_TypeDef* ADCx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the selected ADC automatic injected group conversion */
    ADCx->CR1 |= (uint32_t)ADC_CR1_JAUTO;
  }
  else
  {
    /* Disable the selected ADC automatic injected group conversion */
    ADCx->CR1 &= (uint32_t)(~ADC_CR1_JAUTO);
  }
}

/**
  * @brief  Enables or disables the discontinuous mode for injected group 
  *         channel for the specified ADC
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  NewState: new state of the selected ADC discontinuous mode on injected
  *         group channel.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void ADC_InjectedDiscModeCmd(ADC_TypeDef* ADCx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the selected ADC injected discontinuous mode */
    ADCx->CR1 |= (uint32_t)ADC_CR1_JDISCEN;
  }
  else
  {
    /* Disable the selected ADC injected discontinuous mode */
    ADCx->CR1 &= (uint32_t)(~ADC_CR1_JDISCEN);
  }
}

/**
  * @brief  Returns the ADC injected channel conversion result
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_InjectedChannel: the converted ADC injected channel.
  *          This parameter can be one of the following values:
  *            @arg ADC_InjectedChannel_1: Injected Channel1 selected
  *            @arg ADC_InjectedChannel_2: Injected Channel2 selected
  *            @arg ADC_InjectedChannel_3: Injected Channel3 selected
  *            @arg ADC_InjectedChannel_4: Injected Channel4 selected
  * @retval The Data conversion value.
  */
uint16_t ADC_GetInjectedConversionValue(ADC_TypeDef* ADCx, uint8_t ADC_InjectedChannel)
{
  __IO uint32_t tmp = 0;
  
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_INJECTED_CHANNEL(ADC_InjectedChannel));

  tmp = (uint32_t)ADCx;
  tmp += ADC_InjectedChannel + JDR_OFFSET;
  
  /* Returns the selected injected channel conversion data value */
  return (uint16_t) (*(__IO uint32_t*)  tmp); 
}
/**
  * @}
  */

/** @defgroup ADC_Group7 Interrupts and flags management functions
 *  @brief   Interrupts and flags management functions
 *
@verbatim   
 ===============================================================================
                   Interrupts and flags management functions
 ===============================================================================  

  This section provides functions allowing to configure the ADC Interrupts and 
  to get the status and clear flags and Interrupts pending bits.
  
  Each ADC provides 4 Interrupts sources and 6 Flags which can be divided into 
  3 groups:
  
  I. Flags and Interrupts for ADC regular channels
  =================================================
  Flags :
  ---------- 
     1. ADC_FLAG_OVR : Overrun detection when regular converted data are lost

     2. ADC_FLAG_EOC : Regular channel end of conversion ==> to indicate (depending 
              on EOCS bit, managed by ADC_EOCOnEachRegularChannelCmd() ) the end of:
               ==> a regular CHANNEL conversion 
               ==> sequence of regular GROUP conversions .

     3. ADC_FLAG_STRT: Regular channel start ==> to indicate when regular CHANNEL 
              conversion starts.

  Interrupts :
  ------------
     1. ADC_IT_OVR : specifies the interrupt source for Overrun detection event.  
     2. ADC_IT_EOC : specifies the interrupt source for Regular channel end of 
                     conversion event.
  
  
  II. Flags and Interrupts for ADC Injected channels
  =================================================
  Flags :
  ---------- 
     1. ADC_FLAG_JEOC : Injected channel end of conversion ==> to indicate at 
               the end of injected GROUP conversion  
              
     2. ADC_FLAG_JSTRT: Injected channel start ==> to indicate hardware when 
               injected GROUP conversion starts.

  Interrupts :
  ------------
     1. ADC_IT_JEOC : specifies the interrupt source for Injected channel end of 
                      conversion event.     

  III. General Flags and Interrupts for the ADC
  ================================================= 
  Flags :
  ---------- 
     1. ADC_FLAG_AWD: Analog watchdog ==> to indicate if the converted voltage 
              crosses the programmed thresholds values.
              
  Interrupts :
  ------------
     1. ADC_IT_AWD : specifies the interrupt source for Analog watchdog event. 

  
  The user should identify which mode will be used in his application to manage 
  the ADC controller events: Polling mode or Interrupt mode.
  
  In the Polling Mode it is advised to use the following functions:
      - ADC_GetFlagStatus() : to check if flags events occur. 
      - ADC_ClearFlag()     : to clear the flags events.
      
  In the Interrupt Mode it is advised to use the following functions:
     - ADC_ITConfig()          : to enable or disable the interrupt source.
     - ADC_GetITStatus()       : to check if Interrupt occurs.
     - ADC_ClearITPendingBit() : to clear the Interrupt pending Bit 
                                 (corresponding Flag). 
@endverbatim
  * @{
  */ 
/**
  * @brief  Enables or disables the specified ADC interrupts.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_IT: specifies the ADC interrupt sources to be enabled or disabled. 
  *          This parameter can be one of the following values:
  *            @arg ADC_IT_EOC: End of conversion interrupt mask
  *            @arg ADC_IT_AWD: Analog watchdog interrupt mask
  *            @arg ADC_IT_JEOC: End of injected conversion interrupt mask
  *            @arg ADC_IT_OVR: Overrun interrupt enable                       
  * @param  NewState: new state of the specified ADC interrupts.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void ADC_ITConfig(ADC_TypeDef* ADCx, uint16_t ADC_IT, FunctionalState NewState)  
{
  uint32_t itmask = 0;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  assert_param(IS_ADC_IT(ADC_IT)); 

  /* Get the ADC IT index */
  itmask = (uint8_t)ADC_IT;
  itmask = (uint32_t)0x01 << itmask;    

  if (NewState != DISABLE)
  {
    /* Enable the selected ADC interrupts */
    ADCx->CR1 |= itmask;
  }
  else
  {
    /* Disable the selected ADC interrupts */
    ADCx->CR1 &= (~(uint32_t)itmask);
  }
}

/**
  * @brief  Checks whether the specified ADC flag is set or not.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_FLAG: specifies the flag to check. 
  *          This parameter can be one of the following values:
  *            @arg ADC_FLAG_AWD: Analog watchdog flag
  *            @arg ADC_FLAG_EOC: End of conversion flag
  *            @arg ADC_FLAG_JEOC: End of injected group conversion flag
  *            @arg ADC_FLAG_JSTRT: Start of injected group conversion flag
  *            @arg ADC_FLAG_STRT: Start of regular group conversion flag
  *            @arg ADC_FLAG_OVR: Overrun flag                                                 
  * @retval The new state of ADC_FLAG (SET or RESET).
  */
FlagStatus ADC_GetFlagStatus(ADC_TypeDef* ADCx, uint8_t ADC_FLAG)
{
  FlagStatus bitstatus = RESET;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_GET_FLAG(ADC_FLAG));

  /* Check the status of the specified ADC flag */
  if ((ADCx->SR & ADC_FLAG) != (uint8_t)RESET)
  {
    /* ADC_FLAG is set */
    bitstatus = SET;
  }
  else
  {
    /* ADC_FLAG is reset */
    bitstatus = RESET;
  }
  /* Return the ADC_FLAG status */
  return  bitstatus;
}

/**
  * @brief  Clears the ADCx's pending flags.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_FLAG: specifies the flag to clear. 
  *          This parameter can be any combination of the following values:
  *            @arg ADC_FLAG_AWD: Analog watchdog flag
  *            @arg ADC_FLAG_EOC: End of conversion flag
  *            @arg ADC_FLAG_JEOC: End of injected group conversion flag
  *            @arg ADC_FLAG_JSTRT: Start of injected group conversion flag
  *            @arg ADC_FLAG_STRT: Start of regular group conversion flag
  *            @arg ADC_FLAG_OVR: Overrun flag                          
  * @retval None
  */
void ADC_ClearFlag(ADC_TypeDef* ADCx, uint8_t ADC_FLAG)
{
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_CLEAR_FLAG(ADC_FLAG));

  /* Clear the selected ADC flags */
  ADCx->SR = ~(uint32_t)ADC_FLAG;
}

/**
  * @brief  Checks whether the specified ADC interrupt has occurred or not.
  * @param  ADCx:   where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_IT: specifies the ADC interrupt source to check. 
  *          This parameter can be one of the following values:
  *            @arg ADC_IT_EOC: End of conversion interrupt mask
  *            @arg ADC_IT_AWD: Analog watchdog interrupt mask
  *            @arg ADC_IT_JEOC: End of injected conversion interrupt mask
  *            @arg ADC_IT_OVR: Overrun interrupt mask                        
  * @retval The new state of ADC_IT (SET or RESET).
  */
ITStatus ADC_GetITStatus(ADC_TypeDef* ADCx, uint16_t ADC_IT)
{
  ITStatus bitstatus = RESET;
  uint32_t itmask = 0, enablestatus = 0;

  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_IT(ADC_IT));

  /* Get the ADC IT index */
  itmask = ADC_IT >> 8;

  /* Get the ADC_IT enable bit status */
  enablestatus = (ADCx->CR1 & ((uint32_t)0x01 << (uint8_t)ADC_IT)) ;

  /* Check the status of the specified ADC interrupt */
  if (((ADCx->SR & itmask) != (uint32_t)RESET) && enablestatus)
  {
    /* ADC_IT is set */
    bitstatus = SET;
  }
  else
  {
    /* ADC_IT is reset */
    bitstatus = RESET;
  }
  /* Return the ADC_IT status */
  return  bitstatus;
}

/**
  * @brief  Clears the ADCx's interrupt pending bits.
  * @param  ADCx: where x can be 1, 2 or 3 to select the ADC peripheral.
  * @param  ADC_IT: specifies the ADC interrupt pending bit to clear.
  *          This parameter can be one of the following values:
  *            @arg ADC_IT_EOC: End of conversion interrupt mask
  *            @arg ADC_IT_AWD: Analog watchdog interrupt mask
  *            @arg ADC_IT_JEOC: End of injected conversion interrupt mask
  *            @arg ADC_IT_OVR: Overrun interrupt mask                         
  * @retval None
  */
void ADC_ClearITPendingBit(ADC_TypeDef* ADCx, uint16_t ADC_IT)
{
  uint8_t itmask = 0;
  /* Check the parameters */
  assert_param(IS_ADC_ALL_PERIPH(ADCx));
  assert_param(IS_ADC_IT(ADC_IT)); 
  /* Get the ADC IT index */
  itmask = (uint8_t)(ADC_IT >> 8);
  /* Clear the selected ADC interrupt pending bits */
  ADCx->SR = ~(uint32_t)itmask;
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
